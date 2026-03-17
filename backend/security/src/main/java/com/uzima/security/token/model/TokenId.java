package com.uzima.security.token.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'un token (access ou refresh).
 * Utilisé comme JTI (JWT ID) dans le payload JWT.
 */
public record TokenId(UUID value) {

    public TokenId {
        Objects.requireNonNull(value, "La valeur du TokenId est obligatoire");
    }

    public static TokenId generate() {
        return new TokenId(UUID.randomUUID());
    }

    public static TokenId of(String uuidString) {
        try {
            return new TokenId(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("TokenId invalide : " + uuidString, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
