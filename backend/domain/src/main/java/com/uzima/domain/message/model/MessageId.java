package com.uzima.domain.message.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'un message.
 */
public record MessageId(UUID value) {

    public MessageId {
        Objects.requireNonNull(value, "L'identifiant du message ne peut pas être nul");
    }

    public static MessageId generate() {
        return new MessageId(UUID.randomUUID());
    }

    public static MessageId of(UUID uuid) {
        return new MessageId(uuid);
    }

    public static MessageId of(String uuid) {
        return new MessageId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
