package com.uzima.security.token.model;

import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object : Claims extraits d'un JWT vérifié.
 * Résultat de la vérification de l'access token.
 * Ne contient que ce qui est nécessaire pour l'autorisation.
 */
public record TokenClaims(
        TokenId tokenId,
        UserId userId,
        Instant issuedAt,
        Instant expiresAt
) {
    public TokenClaims {
        Objects.requireNonNull(tokenId, "Le tokenId est obligatoire");
        Objects.requireNonNull(userId, "Le userId est obligatoire");
        Objects.requireNonNull(issuedAt, "La date d'émission est obligatoire");
        Objects.requireNonNull(expiresAt, "La date d'expiration est obligatoire");
    }
}
