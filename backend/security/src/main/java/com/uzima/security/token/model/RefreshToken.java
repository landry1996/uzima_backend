package com.uzima.security.token.model;

import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object / Entité légère : Refresh Token.
 * Longue durée de vie (30 jours par défaut).
 * Stocké en base (haché) pour permettre la révocation.
 * Rotation : chaque utilisation génère un nouveau refresh token et révoque l'ancien.
 * Token Family : détecte les replay attacks (réutilisation d'un token révoqué).
 */
public final class RefreshToken {

    private final TokenId tokenId;
    private final TokenFamily family;
    private final UserId userId;
    private final String hashedValue;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;

    private RefreshToken(
            TokenId tokenId,
            TokenFamily family,
            UserId userId,
            String hashedValue,
            Instant issuedAt,
            Instant expiresAt,
            boolean revoked,
            Instant revokedAt
    ) {
        this.tokenId = Objects.requireNonNull(tokenId);
        this.family = Objects.requireNonNull(family);
        this.userId = Objects.requireNonNull(userId);
        this.hashedValue = Objects.requireNonNull(hashedValue);
        if (hashedValue.isBlank()) throw new IllegalArgumentException("Le hash du token ne peut pas être vide");
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        if (!expiresAt.isAfter(issuedAt)) throw new IllegalArgumentException("L'expiration doit être après l'émission");
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }

    /** Crée un nouveau refresh token (première émission ou rotation). */
    public static RefreshToken issue(
            TokenId tokenId,
            TokenFamily family,
            UserId userId,
            String hashedValue,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return new RefreshToken(tokenId, family, userId, hashedValue, issuedAt, expiresAt, false, null);
    }

    /** Reconstitution depuis la persistance. */
    public static RefreshToken reconstitute(
            TokenId tokenId,
            TokenFamily family,
            UserId userId,
            String hashedValue,
            Instant issuedAt,
            Instant expiresAt,
            boolean revoked,
            Instant revokedAt
    ) {
        return new RefreshToken(tokenId, family, userId, hashedValue, issuedAt, expiresAt, revoked, revokedAt);
    }

    public void revoke(Instant now) {
        if (this.revoked) return; // idempotent
        this.revoked = true;
        this.revokedAt = Objects.requireNonNull(now);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return !revoked && !isExpiredAt(now);
    }

    public TokenId tokenId() { return tokenId; }
    public TokenFamily family() { return family; }
    public UserId userId() { return userId; }
    public String hashedValue() { return hashedValue; }
    public Instant issuedAt() { return issuedAt; }
    public Instant expiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public Instant revokedAt() { return revokedAt; }

    @Override
    public String toString() {
        return "RefreshToken[tokenId=" + tokenId + ", family=" + family + ", userId=" + userId +
               ", revoked=" + revoked + ", expiresAt=" + expiresAt + "]";
    }
}
