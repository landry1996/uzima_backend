package com.uzima.infrastructure.security;

import com.uzima.application.security.LoginAttemptRepositoryPort;
import com.uzima.domain.security.LoginAttempt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implémentation en mémoire du LoginAttemptRepositoryPort.
 * <p>
 * Utilisée en développement et pour les tests d'intégration.
 * <p>
 * PRODUCTION : Remplacer par RedisLoginAttemptRepository :
 * - Persistance cross-instances (load balancing)
 * - TTL automatique (expiration sans garbage collect manuel)
 * - Atomic operations (thread-safety native)
 * <p>
 * Cette implémentation est thread-safe via ConcurrentHashMap + CopyOnWriteArrayList.
 */
public final class InMemoryLoginAttemptRepository implements LoginAttemptRepositoryPort {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<LoginAttempt>> store =
            new ConcurrentHashMap<>();

    @Override
    public void save(LoginAttempt attempt) {
        store.computeIfAbsent(attempt.identifier(), k -> new CopyOnWriteArrayList<>())
             .add(attempt);

        // Nettoyage préventif des vieilles entrées (>25h) pour éviter la fuite mémoire
        purgeExpired(attempt.identifier(), Duration.ofHours(25), attempt.attemptedAt());
    }

    @Override
    public List<LoginAttempt> findRecentAttempts(String identifier, Duration within, Instant from) {
        List<LoginAttempt> attempts = store.getOrDefault(identifier, new CopyOnWriteArrayList<>());
        Instant windowStart = from.minus(within);
        return attempts.stream()
                .filter(a -> a.attemptedAt().isAfter(windowStart))
                .sorted((a, b) -> b.attemptedAt().compareTo(a.attemptedAt()))
                .toList();
    }

    @Override
    public void clearAttempts(String identifier) {
        store.remove(identifier);
    }

    private void purgeExpired(String identifier, Duration maxAge, Instant now) {
        Instant cutoff = now.minus(maxAge);
        store.computeIfPresent(identifier, (k, list) -> {
            list.removeIf(attempt -> attempt.attemptedAt().isBefore(cutoff));
            return list.isEmpty() ? null : list;
        });
    }
}
