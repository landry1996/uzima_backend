package com.uzima.infrastructure.notification;

import com.uzima.application.notification.PendingNotification;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service de flush des notifications différées.
 * <p>
 * Responsabilités :
 * <ol>
 *   <li><b>Flush ciblé</b> : {@link #flushForUser(UserId)} — appelé explicitement
 *       quand un utilisateur repasse à l'état AVAILABLE (futur use case UpdatePresenceStatus).</li>
 *   <li><b>Flush périodique</b> : {@link #flushAll()} — filet de sécurité schedulé
 *       toutes les N secondes (configuré via {@code @Scheduled} dans le bean Spring).</li>
 * </ol>
 * <p>
 * Ce service est découplé du scheduler Spring : il n'a aucune annotation Spring
 * afin de rester testable sans contexte Spring.
 * Le scheduling est configuré dans {@code InfrastructureConfiguration} via un wrapper.
 * <p>
 * Thread-safety : hérite de la thread-safety de {@link DeferredNotificationStrategy}
 * (ConcurrentHashMap + ConcurrentLinkedQueue).
 */
public final class DeferredQueueFlusherService {

    private static final Logger log = LoggerFactory.getLogger(DeferredQueueFlusherService.class);

    private final DeferredNotificationStrategy deferredStrategy;
    private final WebSocketNotifierPort webSocketNotifier;

    public DeferredQueueFlusherService(
            DeferredNotificationStrategy deferredStrategy,
            WebSocketNotifierPort webSocketNotifier
    ) {
        this.deferredStrategy = Objects.requireNonNull(deferredStrategy);
        this.webSocketNotifier = Objects.requireNonNull(webSocketNotifier);
    }

    /**
     * Draine et livre immédiatement toutes les notifications en attente
     * pour un utilisateur donné (ex: il vient de passer à AVAILABLE).
     *
     * @param userId identifiant de l'utilisateur dont la présence vient de changer
     */
    public void flushForUser(UserId userId) {
        Objects.requireNonNull(userId);
        List<PendingNotification> pending = deferredStrategy.drainQueue(userId);
        if (pending.isEmpty()) {
            log.debug("[FLUSHER] Aucune notification en attente pour userId={}", userId.value());
            return;
        }
        log.info("[FLUSHER] Flush ciblé → userId={} : {} notification(s) à livrer", userId.value(), pending.size());
        deliver(userId, pending);
    }

    /**
     * Draine et livre toutes les notifications différées (tous utilisateurs).
     * Appelé périodiquement par le scheduler comme filet de sécurité.
     */
    public void flushAll() {
        Map<UserId, List<PendingNotification>> allPending = deferredStrategy.drainAll();
        if (allPending.isEmpty()) {
            log.debug("[FLUSHER] Flush périodique : aucune notification en attente");
            return;
        }
        int total = allPending.values().stream().mapToInt(List::size).sum();
        log.info("[FLUSHER] Flush périodique → {} utilisateur(s), {} notification(s) à livrer",
                allPending.size(), total);
        allPending.forEach(this::deliver);
    }

    // -------------------------------------------------------------------------
    // Privé
    // -------------------------------------------------------------------------

    private void deliver(UserId userId, List<PendingNotification> notifications) {
        int delivered = 0;
        int failed = 0;
        for (PendingNotification notification : notifications) {
            try {
                webSocketNotifier.sendToUser(userId, notification);
                delivered++;
            } catch (Exception e) {
                // On logue l'erreur mais on continue la livraison des autres notifications.
                // Une notification perdue est préférable à un flush interrompu.
                log.error("[FLUSHER] Échec livraison → userId={} messageId={} : {}",
                        userId.value(),
                        notification.message().id(),
                        e.getMessage(), e);
                failed++;
            }
        }
        if (failed > 0) {
            log.warn("[FLUSHER] Livraison partielle → userId={} : {}/{} réussies, {} échouée(s)",
                    userId.value(), delivered, notifications.size(), failed);
        } else {
            log.info("[FLUSHER] Livraison complète → userId={} : {} notification(s) envoyée(s)",
                    userId.value(), delivered);
        }
    }
}
