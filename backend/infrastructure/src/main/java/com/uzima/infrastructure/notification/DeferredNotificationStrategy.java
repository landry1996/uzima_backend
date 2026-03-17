package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Stratégie : Notification DIFFÉRÉE (batching).
 * <p>
 * Appliquée pour : FOCUSED, TIRED, SILENCE (NotificationPolicy.DEFERRED)
 * <p>
 * Comportement :
 * - Stocke la notification dans une file en mémoire (ConcurrentLinkedQueue)
 *   indexée par userId → "notifications:deferred:{recipientId}"
 * - Livre lors de la prochaine fenêtre batch via drainQueue(recipientId)
 *   (typiquement appelé quand l'état de présence repasse à AVAILABLE)
 * - Un scheduler (DeferredQueueFlusherService) peut aussi déclencher le flush
 *   périodiquement comme filet de sécurité.
 * <p>
 * Résultat visible par l'expéditeur :
 * "Marie est concentrée, message livré silencieusement"
 * <p>
 * Thread-safety : ConcurrentHashMap + ConcurrentLinkedQueue — safe pour accès concurrent.
 * <p>
 * Note production : remplacer ConcurrentLinkedQueue par RedisTemplate<String, PendingNotification>
 * en conservant la même interface (drainQueue, pendingCount, drainAll).
 */
public final class DeferredNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DeferredNotificationStrategy.class);

    // Clé = "notifications:deferred:{recipientId.value()}", valeur = file FIFO
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<PendingNotification>> queues =
            new ConcurrentHashMap<>();

    @Override
    public void route(PendingNotification notification) {
        Objects.requireNonNull(notification);
        String key = queueKey(notification.recipientId());
        queues.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(notification);
        log.info("[DEFERRED] Notification mise en file → userId={} clé='{}' file_size={}",
                notification.recipientId(), key, queues.get(key).size());
    }

    @Override
    public PresenceStatus.NotificationPolicy supportedPolicy() {
        return PresenceStatus.NotificationPolicy.DEFERRED;
    }

    /**
     * Draine et retourne toutes les notifications en attente pour un utilisateur.
     * Appelé lorsque l'utilisateur repasse à l'état AVAILABLE, ou par le scheduler.
     * <p>
     * Atomic : la queue est supprimée de la map avant d'être retournée,
     * ce qui évite les doubles livraisons en cas d'appels concurrents.
     *
     * @param recipientId identifiant du destinataire
     * @return liste ordonnée (FIFO) des notifications en attente, vide si aucune
     */
    public List<PendingNotification> drainQueue(UserId recipientId) {
        Objects.requireNonNull(recipientId);
        String key = queueKey(recipientId);
        ConcurrentLinkedQueue<PendingNotification> queue = queues.remove(key);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        List<PendingNotification> drained = new ArrayList<>(queue);
        log.info("[DEFERRED] {} notification(s) drainée(s) pour userId={}", drained.size(), recipientId);
        return drained;
    }

    /**
     * Draine toutes les queues de tous les utilisateurs.
     * Utilisé par le scheduler périodique (DeferredQueueFlusherService).
     *
     * @return map userId → liste de notifications drainées (seulement les entrées non vides)
     */
    public Map<UserId, List<PendingNotification>> drainAll() {
        if (queues.isEmpty()) {
            return Map.of();
        }
        // Snapshot des clés existantes, puis drain atomique par clé
        Set<String> keys = Set.copyOf(queues.keySet());
        Map<UserId, List<PendingNotification>> result = new HashMap<>();
        for (String key : keys) {
            ConcurrentLinkedQueue<PendingNotification> queue = queues.remove(key);
            if (queue != null && !queue.isEmpty()) {
                List<PendingNotification> batch = new ArrayList<>(queue);
                // Extraire le UserId depuis la première notification de la queue
                UserId userId = batch.getFirst().recipientId();
                result.put(userId, batch);
            }
        }
        int total = result.values().stream().mapToInt(List::size).sum();
        if (total > 0) {
            log.info("[DEFERRED] drainAll() → {} utilisateur(s), {} notification(s) au total",
                    result.size(), total);
        }
        return result;
    }

    /**
     * Retourne le nombre de notifications en attente pour un utilisateur.
     * Utile pour l'affichage du badge de notifications non lues.
     *
     * @param recipientId identifiant du destinataire
     * @return nombre de notifications en attente (0 si aucune)
     */
    public int pendingCount(UserId recipientId) {
        Objects.requireNonNull(recipientId);
        ConcurrentLinkedQueue<PendingNotification> queue = queues.get(queueKey(recipientId));
        return queue == null ? 0 : queue.size();
    }

    /**
     * Retourne le nombre total de notifications en attente (tous utilisateurs confondus).
     * Utilisé par les métriques Micrometer (Gauge).
     *
     * @return total des notifications différées en mémoire
     */
    public int totalPendingCount() {
        return queues.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
    }

    private String queueKey(UserId recipientId) {
        return "notifications:deferred:" + recipientId.value();
    }
}
