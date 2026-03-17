package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.domain.user.model.PresenceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Stratégie : Notification IMMÉDIATE.
 * <p>
 * Appliquée pour : AVAILABLE, CELEBRATING (NotificationPolicy.NORMAL)
 * <p>
 * Comportement : délègue l'envoi temps réel au {@link WebSocketNotifierPort}.
 * En local (profil non-prod), le port est implémenté par {@code LoggingNotificationAdapter}.
 * En prod, par {@code SocketIONotificationAdapter}.
 */
public final class ImmediateNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(ImmediateNotificationStrategy.class);

    private final WebSocketNotifierPort webSocketNotifier;

    public ImmediateNotificationStrategy(WebSocketNotifierPort webSocketNotifier) {
        this.webSocketNotifier = Objects.requireNonNull(webSocketNotifier, "webSocketNotifier obligatoire");
    }

    @Override
    public void route(PendingNotification notification) {
        Objects.requireNonNull(notification);
        try {
            webSocketNotifier.sendToUser(notification.recipientId(), notification);
            log.info("[IMMEDIATE] Notification WebSocket envoyée → userId={} messageId={} type={}",
                    notification.recipientId(),
                    notification.message().id(),
                    notification.type());
        } catch (Exception e) {
            // On logue en error mais on laisse l'exception remonter :
            // PresenceAwareNotificationAdapter catch déjà au niveau supérieur,
            // ce qui évite de bloquer l'envoi du message.
            log.error("[IMMEDIATE] Échec envoi WebSocket → userId={} messageId={} : {}",
                    notification.recipientId(),
                    notification.message().id(),
                    e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public PresenceStatus.NotificationPolicy supportedPolicy() {
        return PresenceStatus.NotificationPolicy.NORMAL;
    }
}
