package com.uzima.application.message.port.in;

import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Démarrage d'une conversation directe.
 */
public record StartConversationCommand(UserId initiatorId, UserId targetId) {

    public StartConversationCommand {
        Objects.requireNonNull(initiatorId, "L'initiateur est obligatoire");
        Objects.requireNonNull(targetId, "Le destinataire est obligatoire");
        if (initiatorId.equals(targetId)) {
            throw new IllegalArgumentException("Un utilisateur ne peut pas démarrer une conversation avec lui-même");
        }
    }
}
