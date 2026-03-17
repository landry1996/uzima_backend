package com.uzima.application.message.port.in;

import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Requête CQRS : Récupération d'une conversation avec ses messages.
 */
public record GetConversationQuery(
        ConversationId conversationId,
        UserId requesterId,
        int limit,
        int offset
) {
    public GetConversationQuery {
        Objects.requireNonNull(conversationId, "La conversation est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("La limite doit être entre 1 et 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("L'offset ne peut pas être négatif");
        }
    }
}
