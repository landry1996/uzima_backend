package com.uzima.infrastructure.notification;

import com.uzima.application.message.port.out.MessageNotificationPort;
import com.uzima.application.notification.NotificationRouter;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

/**
 * Adaptateur : Implémente MessageNotificationPort avec routing basé sur PresenceStatus.
 * <p>
 * Principe (Respect Automatique des États) :
 * 1. Pour chaque destinataire, récupère son PresenceStatus courant
 * 2. Délègue au NotificationRouter qui sélectionne la stratégie appropriée
 * 3. La stratégie choisie livre la notification de façon adaptée à l'état
 * <p>
 * L'application ne connaît que MessageNotificationPort — transparence totale.
 * Une erreur de notification ne bloque jamais l'envoi du message.
 */
public final class PresenceAwareNotificationAdapter implements MessageNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(PresenceAwareNotificationAdapter.class);

    private final NotificationRouter router;
    private final UserRepositoryPort userRepository;

    public PresenceAwareNotificationAdapter(
            NotificationRouter router,
            UserRepositoryPort userRepository
    ) {
        this.router = Objects.requireNonNull(router);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    public void notifyNewMessage(Message message, Set<UserId> recipients) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(recipients);

        for (UserId recipientId : recipients) {
            try {
                PresenceStatus status = userRepository.findById(recipientId)
                        .map(User::presenceStatus)
                        .orElse(PresenceStatus.OFFLINE);

                router.route(message, recipientId, status);

            } catch (Exception e) {
                // Isolation : une erreur de notification ne doit jamais bloquer l'envoi du message.
                log.error("[NOTIFICATION] Échec de notification → userId={} messageId={} : {}",
                        recipientId,
                        message.id(),
                        e.getMessage(), e);
            }
        }
    }
}
