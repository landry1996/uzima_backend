package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Stratégie : Notification BLOQUÉE.
 * <p>
 * Appliquée pour : WELLNESS, OFFLINE (NotificationPolicy.BLOCKED)
 * <p>
 * Comportement :
 * - Aucune notification temps réel (ni WebSocket, ni file)
 * - Le message est déjà persisté en DB par MessageRepositoryPort.save()
 * - L'utilisateur verra ses messages non lus à son prochain login/retour AVAILABLE
 * <p>
 * Noteworthy : BLOCKED ne signifie pas "message perdu" — l'utilisateur retrouve
 * tout en base à son retour. Seul l'envoi temps réel est bloqué.
 */
public final class BlockedNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(BlockedNotificationStrategy.class);

    @Override
    public void route(PendingNotification notification) {
        Objects.requireNonNull(notification);
        // Message déjà persisté en DB — aucune action temps réel délibérée.
        log.info("[BLOCKED] Notification temps réel supprimée → userId={} messageId={} (WELLNESS/OFFLINE — message disponible en DB)",
                notification.recipientId(),
                notification.message().id());
    }

    @Override
    public PresenceStatus.NotificationPolicy supportedPolicy() {
        return PresenceStatus.NotificationPolicy.BLOCKED;
    }
}
