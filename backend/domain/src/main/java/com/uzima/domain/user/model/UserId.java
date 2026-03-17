package com.uzima.domain.user.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'un utilisateur.
 * <p>
 * Immuable. Deux UserId avec le même UUID sont égaux.
 * Impossible de créer un UserId avec une valeur nulle.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "L'identifiant utilisateur ne peut pas être nul");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String uuid) {
        try {
            return new UserId(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            throw new InvalidUserIdException("Format UUID invalide : " + uuid);
        }
    }

    public static UserId of(UUID uuid) {
        return new UserId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static final class InvalidUserIdException extends RuntimeException {
        public InvalidUserIdException(String message) {
            super(message);
        }
    }
}
