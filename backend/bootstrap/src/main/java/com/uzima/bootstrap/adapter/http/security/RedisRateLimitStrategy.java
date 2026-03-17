package com.uzima.bootstrap.adapter.http.security;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Stratégie de rate limiting distribuée via Redis.
 * <p>
 * Utilisée en production pour garantir un rate limiting cohérent
 * sur toutes les instances (pods Kubernetes, workers, etc.).
 * <p>
 * Algorithme : fenêtre fixe par bucket temporel.
 * <pre>
 *   Clé Redis : uzima:rl:{clientIp}:{windowBucket}
 *   windowBucket = currentTimeMs / windowMs  (identifie la fenêtre courante)
 * </pre>
 * Chaque clé expire automatiquement après 2 × windowDuration pour éviter
 * l'accumulation de clés orphelines sans impacter la précision du compteur.
 * <p>
 * Intégration WAF : les décisions de blocage sont prises en amont par le WAF
 * (Cloudflare, AWS WAF, Azure Front Door). Ce filtre constitue la deuxième
 * ligne de défense au niveau applicatif.
 */
public final class RedisRateLimitStrategy implements RateLimitStrategy {

    private static final String KEY_PREFIX = "uzima:rl:";

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate est obligatoire");
    }

    @Override
    public long increment(String clientIp, long windowMs) {
        // Bucket = index de la fenêtre courante (change toutes les windowMs millisecondes)
        long windowBucket = System.currentTimeMillis() / windowMs;
        String key = KEY_PREFIX + clientIp + ":" + windowBucket;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            // Redis indisponible : fail-open (laisser passer) pour ne pas bloquer
            // le service entier. Journaliser en production pour alerter sur Redis.
            return 0L;
        }

        // Positionner l'expiration uniquement à la première requête de la fenêtre
        // (INCR atomique : si count == 1, on vient de créer la clé)
        if (count == 1L) {
            long expirySeconds = (windowMs / 1_000L) * 2;
            redisTemplate.expire(key, expirySeconds, TimeUnit.SECONDS);
        }

        return count;
    }
}
