package com.uzima.application.notification;

import com.uzima.domain.user.model.UserId;

/**
 * Port de sortie : envoi WebSocket d'une notification vers un utilisateur connecté.
 * <p>
 * Implémentations :
 * - {@code SocketIONotificationAdapter}  → prod (Socket.IO via Netty)
 * - {@code LoggingNotificationAdapter}   → local/test (log structuré, aucune dépendance)
 * <p>
 * Ce port isole les stratégies de notification de tout détail technique WebSocket.
 */
public interface WebSocketNotifierPort {

    /**
     * Envoie une notification en temps réel à un utilisateur connecté.
     * <p>
     * L'implémentation ne doit pas lever d'exception bloquante si l'utilisateur
     * n'est pas connecté : la notification est silencieusement ignorée.
     *
     * @param recipientId identifiant du destinataire
     * @param notification notification à envoyer
     */
    void sendToUser(UserId recipientId, PendingNotification notification);
}
