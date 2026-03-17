package com.uzima.security.token.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Famille de tokens.
 * Tous les refresh tokens issus d'une même session partagent le même familyId.
 * Si un refresh token révoqué est réutilisé (replay attack),
 * TOUTE la famille est invalidée immédiatement.
 * Pattern : Token Rotation + Token Family (RFC recommandation OAuth2)
 */
public record TokenFamily(UUID value) {

    public TokenFamily {
        Objects.requireNonNull(value, "La valeur de la famille est obligatoire");
    }

    public static TokenFamily generate() {
        return new TokenFamily(UUID.randomUUID());
    }

    public static TokenFamily of(String uuidString) {
        try {
            return new TokenFamily(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("TokenFamily invalide : " + uuidString, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
