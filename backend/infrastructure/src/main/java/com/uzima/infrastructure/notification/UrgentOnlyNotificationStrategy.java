package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Stratégie : Notification pour URGENCES UNIQUEMENT.
 * <p>
 * Appliquée pour : TRAVELING, PHYSICAL_ACTIVITY, SLEEPING (NotificationPolicy.URGENT_ONLY)
 * <p>
 * Comportement :
 * - Si la notification est urgente (URGENT_MESSAGE) → envoi immédiat via WebSocket
 * - Si la notification est normale (NEW_MESSAGE) → mise en file différée
 * <p>
 * La priorité "urgente" est définie dans la couche application (NotificationType).
 * Cette stratégie délègue sans contenir de logique métier propre.
 */
public final class UrgentOnlyNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(UrgentOnlyNotificationStrategy.class);

    private final ImmediateNotificationStrategy immediateStrategy;
    private final DeferredNotificationStrategy deferredStrategy;

    public UrgentOnlyNotificationStrategy(
            ImmediateNotificationStrategy immediateStrategy,
            DeferredNotificationStrategy deferredStrategy
    ) {
        this.immediateStrategy = Objects.requireNonNull(immediateStrategy);
        this.deferredStrategy = Objects.requireNonNull(deferredStrategy);
    }

    @Override
    public void route(PendingNotification notification) {
        Objects.requireNonNull(notification);
        if (notification.isUrgent()) {
            log.info("[URGENT_ONLY] Notification urgente → envoi immédiat userId={}",
                    notification.recipientId());
            immediateStrategy.route(notification);
        } else {
            log.info("[URGENT_ONLY] Notification normale → mise en file userId={}",
                    notification.recipientId());
            deferredStrategy.route(notification);
        }
    }

    @Override
    public PresenceStatus.NotificationPolicy supportedPolicy() {
        return PresenceStatus.NotificationPolicy.URGENT_ONLY;
    }
}
