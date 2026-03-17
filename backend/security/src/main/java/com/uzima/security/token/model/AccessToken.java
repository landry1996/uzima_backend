package com.uzima.security.token.model;

import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object : Access Token JWT.
 * Courte durée de vie (15 min par défaut).
 * Transporté dans l'en-tête Authorization: Bearer <token>.
 * Stateless : vérifié par signature, pas de lookup en base.
 * Immuable par record.
 */
public record AccessToken(
        TokenId tokenId,
        UserId userId,
        String rawValue,
        Instant issuedAt,
        Instant expiresAt
) {
    public AccessToken {
        Objects.requireNonNull(tokenId, "Le tokenId est obligatoire");
        Objects.requireNonNull(userId, "Le userId est obligatoire");
        Objects.requireNonNull(rawValue, "La valeur brute du token est obligatoire");
        if (rawValue.isBlank()) throw new IllegalArgumentException("La valeur du token ne peut pas être vide");
        Objects.requireNonNull(issuedAt, "La date d'émission est obligatoire");
        Objects.requireNonNull(expiresAt, "La date d'expiration est obligatoire");
        if (!expiresAt.isAfter(issuedAt)) throw new IllegalArgumentException("L'expiration doit être après l'émission");
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Masque la valeur brute pour les logs. */
    @Override
    public String toString() {
        return "AccessToken[tokenId=" + tokenId + ", userId=" + userId + ", expiresAt=" + expiresAt + "]";
    }
}
