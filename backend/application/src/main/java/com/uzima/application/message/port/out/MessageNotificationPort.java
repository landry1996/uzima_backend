package com.uzima.application.message.port.out;

import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.UserId;

import java.util.Set;

/**
 * Port de sortie : notification en temps réel lors de l'envoi d'un message.
 * <p>
 * L'application ne connaît pas WebSocket ou Socket.IO.
 * L'infrastructure implémente ce port avec la technologie choisie.
 */
public interface MessageNotificationPort {

    /**
     * Notifie les destinataires qu'un nouveau message a été envoyé.
     *
     * @param message     Le message envoyé
     * @param recipients  Les utilisateurs à notifier (en excluant l'expéditeur)
     */
    void notifyNewMessage(Message message, Set<UserId> recipients);
}
