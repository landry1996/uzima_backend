package com.uzima.application.notification;

import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Value Object : Notification en attente de routage.
 * Transporte les informations nécessaires à toutes les stratégies
 * de notification sans dépendre de l'infrastructure.
 * Immuable par record.
 */
public record PendingNotification(
        Message message,
        UserId recipientId,
        NotificationType type
) {
    public PendingNotification {
        Objects.requireNonNull(message, "Le message est obligatoire");
        Objects.requireNonNull(recipientId, "Le destinataire est obligatoire");
        Objects.requireNonNull(type, "Le type de notification est obligatoire");
    }

    public enum NotificationType {
        /** Message standard entre participants */
        NEW_MESSAGE,
        /** Message urgent (ex: contact prioritaire, SOS) */
        URGENT_MESSAGE
    }

    public boolean isUrgent() {
        return type == NotificationType.URGENT_MESSAGE;
    }
}
