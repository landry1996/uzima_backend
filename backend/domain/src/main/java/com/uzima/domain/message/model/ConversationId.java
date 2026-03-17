package com.uzima.domain.message.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'une conversation.
 */
public record ConversationId(UUID value) {

    public ConversationId {
        Objects.requireNonNull(value, "L'identifiant de la conversation ne peut pas être nul");
    }

    public static ConversationId generate() {
        return new ConversationId(UUID.randomUUID());
    }

    public static ConversationId of(UUID uuid) {
        return new ConversationId(uuid);
    }

    public static ConversationId of(String uuid) {
        return new ConversationId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
