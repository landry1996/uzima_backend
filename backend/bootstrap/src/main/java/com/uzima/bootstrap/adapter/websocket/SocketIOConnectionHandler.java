package com.uzima.bootstrap.adapter.websocket;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Gestionnaire de connexions Socket.IO.
 * <p>
 * À la connexion, le client DOIT passer son userId en query param :
 * {@code ws://host:9092?userId=<uuid>}
 * <p>
 * Le client rejoint une room nommée par son userId, ce qui permet à
 * {@link SocketIONotificationAdapter} de cibler précisément un utilisateur.
 * <p>
 * Sécurité production : remplacer la lecture du query param par validation JWT.
 * Exemple : client envoie {@code ?token=<jwt>}, ce handler le valide et extrait l'userId.
 * Pour l'instant, on fait confiance au paramètre en local (pas de prod).
 */
public final class SocketIOConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(SocketIOConnectionHandler.class);
    private static final String USER_ID_PARAM = "userId";

    private final SocketIOServer server;

    public SocketIOConnectionHandler(SocketIOServer server) {
        this.server = Objects.requireNonNull(server);
        registerListeners();
    }

    private void registerListeners() {
        server.addConnectListener(onConnect());
        server.addDisconnectListener(onDisconnect());
    }

    private ConnectListener onConnect() {
        return client -> {
            String userId = client.getHandshakeData().getSingleUrlParam(USER_ID_PARAM);
            if (userId == null || userId.isBlank()) {
                log.warn("[SOCKET.IO] Connexion refusée : paramètre '{}' absent (sessionId={})",
                        USER_ID_PARAM, client.getSessionId());
                client.disconnect();
                return;
            }
            client.joinRoom(userId);
            log.info("[SOCKET.IO] Client connecté → userId={} sessionId={}", userId, client.getSessionId());
        };
    }

    private DisconnectListener onDisconnect() {
        return client -> {
            String userId = client.getHandshakeData().getSingleUrlParam(USER_ID_PARAM);
            log.info("[SOCKET.IO] Client déconnecté → userId={} sessionId={}", userId, client.getSessionId());
        };
    }
}
