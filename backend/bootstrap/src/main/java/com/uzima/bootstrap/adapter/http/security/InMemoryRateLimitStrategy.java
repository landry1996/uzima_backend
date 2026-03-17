package com.uzima.bootstrap.adapter.http.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stratégie de rate limiting en mémoire (par instance JVM).
 * <p>
 * Utilisée en développement et en environnement mono-instance.
 * Ne pas utiliser en production multi-instances : chaque pod JVM aurait ses
 * propres compteurs, rendant le rate limiting inefficace.
 * <p>
 * Algorithme : fenêtre fixe (fixed window counter).
 * Structure : {@code ip → [requestCount, windowStartMs]}
 */
public final class InMemoryRateLimitStrategy implements RateLimitStrategy {

    // ip → [count, windowStartMs]
    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    public long increment(String clientIp, long windowMs) {
        long now = System.currentTimeMillis();
        long[] data = counters.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing[1] > windowMs) {
                return new long[]{1L, now};
            }
            existing[0]++;
            return existing;
        });
        return data[0];
    }
}
