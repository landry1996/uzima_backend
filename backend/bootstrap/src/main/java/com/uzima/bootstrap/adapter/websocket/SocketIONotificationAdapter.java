package com.uzima.bootstrap.adapter.websocket;

import com.corundumstudio.socketio.SocketIOServer;
import com.uzima.application.notification.PendingNotification;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Adaptateur WebSocket production : envoie les notifications via Socket.IO (Netty).
 * <p>
 * Actif uniquement avec le profil Spring {@code prod}.
 * <p>
 * Protocole :
 * - Chaque utilisateur rejoint une room nommée par son userId (UUID string)
 *   lors de la connexion (voir {@link SocketIOConnectionHandler}).
 * - L'envoi cible la room → tous les onglets/appareils connectés reçoivent l'événement.
 * - Si l'utilisateur n'est pas connecté, Socket.IO ignore silencieusement l'envoi
 *   (la room est vide — aucune exception).
 * <p>
 * Événement client : {@code "new_message"}
 * Payload : {@link NotificationPayload}
 */
public final class SocketIONotificationAdapter implements WebSocketNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(SocketIONotificationAdapter.class);
    private static final String EVENT_NEW_MESSAGE = "new_message";

    private final SocketIOServer server;

    public SocketIONotificationAdapter(SocketIOServer server) {
        this.server = Objects.requireNonNull(server, "SocketIOServer obligatoire");
    }

    @Override
    public void sendToUser(UserId recipientId, PendingNotification notification) {
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(notification);

        String room = recipientId.value().toString();
        NotificationPayload payload = new NotificationPayload(
                notification.message().id().value().toString(),
                notification.message().content().text(),
                notification.type().name()
        );

        server.getRoomOperations(room).sendEvent(EVENT_NEW_MESSAGE, payload);

        log.info("[SOCKET.IO] Événement '{}' envoyé → room={} messageId={} type={}",
                EVENT_NEW_MESSAGE, room, notification.message().id(), notification.type());
    }

    /**
     * Payload sérialisé en JSON et envoyé au client Socket.IO.
     *
     * @param messageId UUID du message
     * @param content   contenu textuel (tronqué si nécessaire côté client)
     * @param type      NEW_MESSAGE | URGENT_MESSAGE
     */
    public record NotificationPayload(String messageId, String content, String type) {}
}
