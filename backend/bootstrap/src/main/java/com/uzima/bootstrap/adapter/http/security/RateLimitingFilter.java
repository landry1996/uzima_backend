package com.uzima.bootstrap.adapter.http.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Filtre HTTP : Rate Limiting par adresse IP, configurable et distribuable.
 * <p>
 * Double protection contre la force brute :
 * 1. {@code BruteForceProtectionService} : par identifiant (numéro de téléphone)
 * 2. {@code RateLimitingFilter} : par adresse IP (couche HTTP)
 * <p>
 * Stratégie injectable via {@link RateLimitStrategy} :
 * - {@link InMemoryRateLimitStrategy} : dev/local (par instance JVM)
 * - {@link RedisRateLimitStrategy}    : prod (cross-instances, activé avec @Profile("prod"))
 * <p>
 * Intégration WAF :
 * Les WAF (Cloudflare, AWS WAF, Azure Front Door) peuvent injecter un header de score
 * de menace dans la requête. Si ce score dépasse le seuil configuré, la requête est
 * bloquée immédiatement (avant même le rate limiting). Le header est configurable via
 * {@code uzima.security.rate-limit.waf-score-header} et le seuil via
 * {@code uzima.security.rate-limit.waf-score-threshold}.
 * <p>
 * Headers de réponse standard ajoutés sur chaque requête filtrée :
 * - {@code X-RateLimit-Limit}     : limite maximale de requêtes par fenêtre
 * - {@code X-RateLimit-Remaining} : requêtes restantes dans la fenêtre courante
 * - {@code Retry-After}           : délai d'attente (secondes) en cas de 429
 * <p>
 * Configuration via {@code application.yml} :
 * <pre>
 * uzima:
 *   security:
 *     rate-limit:
 *       max-requests: 20
 *       window-seconds: 60
 *       path-prefix: /api/auth
 *       waf-score-header: X-WAF-Score
 *       waf-score-threshold: 50
 * </pre>
 */
public final class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitStrategy strategy;
    private final int              maxRequests;
    private final long             windowMs;
    private final String           pathPrefix;
    private final String           wafScoreHeader;
    private final int              wafScoreThreshold;

    public RateLimitingFilter(
            RateLimitStrategy strategy,
            int               maxRequests,
            int               windowSeconds,
            String            pathPrefix,
            String            wafScoreHeader,
            int               wafScoreThreshold
    ) {
        this.strategy          = Objects.requireNonNull(strategy);
        this.maxRequests       = maxRequests;
        this.windowMs          = (long) windowSeconds * 1_000L;
        this.pathPrefix        = Objects.requireNonNull(pathPrefix);
        this.wafScoreHeader    = Objects.requireNonNull(wafScoreHeader);
        this.wafScoreThreshold = wafScoreThreshold;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(pathPrefix)) {
            chain.doFilter(request, response);
            return;
        }

        // --- 1. Vérification WAF -----------------------------------------------
        // Si le WAF a calculé un score de menace supérieur au seuil, on bloque
        // immédiatement sans consommer de quota de rate limiting.
        if (isBlockedByWaf(request)) {
            sendWafBlockResponse(response);
            return;
        }

        // --- 2. Rate limiting ---------------------------------------------------
        String clientIp  = extractClientIp(request);
        long   count     = strategy.increment(clientIp, windowMs);
        long   remaining = Math.max(0L, maxRequests - count);

        response.setHeader("X-RateLimit-Limit",     String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (count > maxRequests) {
            sendRateLimitResponse(response);
            return;
        }

        chain.doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // WAF
    // -------------------------------------------------------------------------

    /**
     * Retourne {@code true} si le WAF upstream a signalé une menace via header.
     * <p>
     * Compatibilité :
     * - Cloudflare : {@code CF-Threat-Score} (0–100, seuil recommandé : 10)
     * - AWS WAF    : {@code x-amzn-waf-action} (valeur "BLOCK" → seuil = 0)
     * - Générique  : {@code X-WAF-Score} configurable
     */
    private boolean isBlockedByWaf(HttpServletRequest request) {
        String headerValue = request.getHeader(wafScoreHeader);
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(headerValue.strip()) >= wafScoreThreshold;
        } catch (NumberFormatException ignored) {
            // Header présent mais non numérique (ex : AWS "BLOCK") → bloquer par précaution
            return true;
        }
    }

    private void sendWafBlockResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":403,"errorCode":"WAF_BLOCKED",\
                "message":"Requête bloquée par le pare-feu applicatif."}
                """);
    }

    // -------------------------------------------------------------------------
    // Rate limit response
    // -------------------------------------------------------------------------

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        long retryAfterSeconds = windowMs / 1_000L;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(String.format("""
                {"status":429,"errorCode":"RATE_LIMIT_EXCEEDED",\
                "message":"Trop de tentatives. R\\u00e9essayez dans %d secondes."}
                """, retryAfterSeconds));
    }

    // -------------------------------------------------------------------------
    // IP extraction
    // -------------------------------------------------------------------------

    /**
     * Extrait l'IP réelle du client en tenant compte des reverse proxies.
     * <p>
     * Priorité : X-Forwarded-For → X-Real-IP → remoteAddr.
     * La validation de l'IP est volontairement permissive : en cas d'IP invalide
     * ou vide, on retourne "unknown" pour éviter tout contournement par header forgé.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String ip = forwarded.split(",")[0].strip();
            if (!ip.isEmpty()) return ip;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.strip();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "unknown";
    }
}
