package com.uzima.application.notification;

import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.UserId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Routeur de notifications basé sur le pattern Strategy.
 * Sélectionne la stratégie appropriée selon la NotificationPolicy
 * du PresenceStatus du destinataire.
 * Ce composant appartient à l'application car il coordonne des ports,
 * mais ne contient aucune logique infrastructure.
 * Immutabilité : le routeur est construit une fois et partagé.
 */
public final class NotificationRouter {

    private final Map<PresenceStatus.NotificationPolicy, NotificationRoutingStrategy> strategies;
    private final NotificationRoutingStrategy defaultStrategy;

    /**
     * @param strategyList   Liste des stratégies disponibles (une par NotificationPolicy)
     * @param defaultStrategy Stratégie de repli si une politique n'est pas couverte
     */
    public NotificationRouter(
            List<NotificationRoutingStrategy> strategyList,
            NotificationRoutingStrategy defaultStrategy
    ) {
        Objects.requireNonNull(strategyList, "La liste de stratégies ne peut pas être nulle");
        Objects.requireNonNull(defaultStrategy, "La stratégie par défaut est obligatoire");

        Map<PresenceStatus.NotificationPolicy, NotificationRoutingStrategy> map =
                new EnumMap<>(PresenceStatus.NotificationPolicy.class);

        for (NotificationRoutingStrategy strategy : strategyList) {
            map.put(strategy.supportedPolicy(), strategy);
        }

        this.strategies = Map.copyOf(map);
        this.defaultStrategy = defaultStrategy;
    }

    /**
     * Route une notification vers le destinataire selon son état de présence.
     *
     * @param message           Le message envoyé
     * @param recipientId       L'identifiant du destinataire
     * @param recipientStatus   L'état de présence actuel du destinataire
     */
    public void route(Message message, UserId recipientId, PresenceStatus recipientStatus) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(recipientStatus);

        PresenceStatus.NotificationPolicy policy = recipientStatus.notificationPolicy();
        NotificationRoutingStrategy strategy = strategies.getOrDefault(policy, defaultStrategy);

        PendingNotification notification = new PendingNotification(
                message,
                recipientId,
                PendingNotification.NotificationType.NEW_MESSAGE
        );

        strategy.route(notification);
    }

    /**
     * Route une notification urgente — bypasse les politiques DEFERRED.
     * Utilisé pour les SOS, alertes famille, etc.
     */
    public void routeUrgent(Message message, UserId recipientId, PresenceStatus recipientStatus) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(recipientStatus);

        // Pour URGENT : on utilise NORMAL ou URGENT_ONLY → jamais BLOCKED/DEFERRED
        PresenceStatus.NotificationPolicy policy = recipientStatus.notificationPolicy();
        if (policy == PresenceStatus.NotificationPolicy.BLOCKED) {
            // Même WELLNESS/OFFLINE reçoit les urgences
            policy = PresenceStatus.NotificationPolicy.NORMAL;
        }

        NotificationRoutingStrategy strategy = strategies.getOrDefault(policy, defaultStrategy);
        PendingNotification notification = new PendingNotification(
                message,
                recipientId,
                PendingNotification.NotificationType.URGENT_MESSAGE
        );
        strategy.route(notification);
    }
}
