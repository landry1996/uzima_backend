package com.uzima.bootstrap.adapter.security;

import com.uzima.security.token.port.RefreshTokenRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Scheduler Spring : suppression périodique des refresh tokens expirés.
 * <p>
 * Les refresh tokens expirés ne peuvent plus être utilisés, mais restent en base
 * jusqu'à ce que cette tâche de maintenance les supprime.
 * <p>
 * Intervalle configurable via {@code uzima.security.token.cleanup-interval-ms}.
 * Défaut : 24 heures.
 */
@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public TokenCleanupScheduler(RefreshTokenRepositoryPort refreshTokenRepository) {
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
    }

    @Scheduled(fixedDelayString = "${uzima.security.token.cleanup-interval-ms:86400000}")
    public void cleanupExpiredTokens() {
        log.info("[SCHEDULER] Suppression des refresh tokens expirés");
        refreshTokenRepository.deleteExpired();
        log.debug("[SCHEDULER] Nettoyage des tokens expirés terminé");
    }
}
