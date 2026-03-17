package com.uzima.infrastructure.notification;

import com.uzima.application.notification.PendingNotification;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Adaptateur WebSocket no-op pour le profil local/test.
 * <p>
 * Simule l'envoi WebSocket par un log structuré — aucune dépendance externe requise.
 * Actif via le profil Spring {@code !prod} (tout sauf production).
 * <p>
 * Utile pour :
 * - Développement local sans serveur Socket.IO
 * - Tests unitaires et d'intégration
 * - CI/CD sans infrastructure WebSocket
 */
public final class LoggingNotificationAdapter implements WebSocketNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public void sendToUser(UserId recipientId, PendingNotification notification) {
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(notification);
        log.info("[NOOP-WS] Notification simulée → userId={} messageId={} type={} contenu='{}'",
                recipientId.value(),
                notification.message().id(),
                notification.type(),
                abbreviate(notification.message().content().text()));
    }

    private static String abbreviate(String text) {
        if (text == null) return "";
        return text.length() <= 80 ? text : text.substring(0, 80) + "…";
    }
}
