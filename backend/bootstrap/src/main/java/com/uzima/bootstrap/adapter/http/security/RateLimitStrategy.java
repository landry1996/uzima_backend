package com.uzima.bootstrap.adapter.http.security;

/**
 * Port secondaire : Stratégie de rate limiting.
 * <p>
 * Deux implémentations :
 * - {@link InMemoryRateLimitStrategy} : dev/local, compteurs en mémoire (par instance JVM)
 * - {@link RedisRateLimitStrategy}    : prod, compteurs partagés via Redis (cross-instances)
 */
public interface RateLimitStrategy {

    /**
     * Incrémente le compteur de requêtes pour l'IP donnée dans la fenêtre temporelle courante.
     *
     * @param clientIp  Adresse IP du client
     * @param windowMs  Durée de la fenêtre en millisecondes
     * @return          Nombre de requêtes pour cette IP dans la fenêtre courante (après incrément)
     */
    long increment(String clientIp, long windowMs);
}
