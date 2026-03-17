package com.uzima.bootstrap.adapter.http.security;

import com.uzima.security.token.model.TokenClaims;
import com.uzima.security.token.port.AccessTokenVerifierPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtre Spring Security : Validation des access tokens JWT.
 * <p>
 * Pour chaque requête :
 * 1. Extrait le token de l'en-tête Authorization: Bearer <token>
 * 2. Vérifie la signature et l'expiration via AccessTokenVerifierPort
 * 3. Alimente le SecurityContext avec l'identité de l'utilisateur
 * <p>
 * En cas de token invalide : on passe sans authentifier (401 généré par Spring Security).
 * En cas d'absence : même comportement (les endpoints publics n'en ont pas besoin).
 */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final AccessTokenVerifierPort tokenVerifier;

    public JwtAuthenticationFilter(AccessTokenVerifierPort tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = authHeader.substring(BEARER_PREFIX.length());

        try {
            TokenClaims claims = tokenVerifier.verify(rawToken);
            // Principal = userId (String), accessible via @AuthenticationPrincipal
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    claims.userId().toString(),
                    null,
                    Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (AccessTokenVerifierPort.InvalidTokenException e) {
            // Token invalide : on continue sans authentifier → Spring Security refusera si l'endpoint est protégé
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
