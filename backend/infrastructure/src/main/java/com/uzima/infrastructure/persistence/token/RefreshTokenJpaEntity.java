package com.uzima.infrastructure.persistence.token;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'refresh_tokens'.
 * Stocke les refresh tokens hachés (jamais en clair).
 * Utilisé pour la rotation et la révocation.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_hashed_value", columnList = "hashed_value"),
    @Index(name = "idx_refresh_tokens_family", columnList = "family_id"),
    @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID tokenId;

    @Column(name = "family_id", nullable = false, columnDefinition = "uuid")
    private UUID familyId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "hashed_value", nullable = false, unique = true, length = 64)
    private String hashedValue;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static RefreshTokenJpaEntity of(
            UUID tokenId,
            UUID familyId,
            UUID userId,
            String hashedValue,
            Instant issuedAt,
            Instant expiresAt,
            boolean revoked,
            Instant revokedAt
    ) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.tokenId = tokenId;
        entity.familyId = familyId;
        entity.userId = userId;
        entity.hashedValue = hashedValue;
        entity.issuedAt = issuedAt;
        entity.expiresAt = expiresAt;
        entity.revoked = revoked;
        entity.revokedAt = revokedAt;
        return entity;
    }

    public void markRevoked(Instant revokedAt) {
        this.revoked = true;
        this.revokedAt = revokedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshTokenJpaEntity r)) return false;
        return tokenId != null && tokenId.equals(r.tokenId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
