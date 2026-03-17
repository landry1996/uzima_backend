package com.uzima.bootstrap.adapter.websocket;

import com.uzima.infrastructure.notification.DeferredQueueFlusherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Scheduler Spring : déclenche le flush périodique des notifications différées.
 * <p>
 * Séparé de {@code InfrastructureConfiguration} pour que le scheduling soit
 * un composant Spring standard (pas de conflit avec la configuration de contexte).
 * <p>
 * Intervalle configurable via {@code uzima.notification.deferred.flush-interval-ms}.
 * Défaut : 60 secondes.
 * <p>
 * Ce scheduler est un filet de sécurité : dans l'idéal, le flush est déclenché
 * précisément par {@code DeferredQueueFlusherService#flushForUser(UserId)}
 * quand un utilisateur repasse à AVAILABLE.
 */
@Component
public class DeferredFlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeferredFlushScheduler.class);

    private final DeferredQueueFlusherService flusher;

    public DeferredFlushScheduler(DeferredQueueFlusherService flusher) {
        this.flusher = Objects.requireNonNull(flusher);
    }

    @Scheduled(fixedDelayString = "${uzima.notification.deferred.flush-interval-ms:60000}")
    public void flushDeferred() {
        log.debug("[SCHEDULER] Flush périodique des notifications différées");
        flusher.flushAll();
    }
}
