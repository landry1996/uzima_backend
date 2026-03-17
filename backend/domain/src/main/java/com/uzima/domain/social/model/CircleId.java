package com.uzima.domain.social.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'un Cercle de Vie.
 * Immuable. Deux CircleId avec le même UUID sont égaux.
 */
public record CircleId(UUID value) {

    public CircleId {
        Objects.requireNonNull(value, "L'identifiant de cercle ne peut pas être nul");
    }

    public static CircleId generate() {
        return new CircleId(UUID.randomUUID());
    }

    public static CircleId of(UUID uuid) {
        return new CircleId(uuid);
    }

    public static CircleId of(String uuid) {
        try {
            return new CircleId(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            throw new InvalidCircleIdException("Format UUID invalide : " + uuid);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static final class InvalidCircleIdException extends RuntimeException {
        public InvalidCircleIdException(String message) {
            super(message);
        }
    }
}
