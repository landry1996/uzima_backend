package com.uzima.infrastructure.persistence.token;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.model.TokenFamily;
import com.uzima.security.token.model.TokenId;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Persistance des refresh tokens via JPA.
 * Implémente RefreshTokenRepositoryPort du module security.
 */
public final class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final SpringDataRefreshTokenRepository jpaRepository;

    public JpaRefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(RefreshToken refreshToken) {
        jpaRepository.save(toEntity(refreshToken));
    }

    @Override
    public void update(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            // Mise à jour partielle : seuls revoked + revokedAt changent
            // → utilise markRevoked() pour éviter un remplacement complet de l'entité
            jpaRepository.findById(refreshToken.tokenId().value())
                    .ifPresent(entity -> {
                        entity.markRevoked(refreshToken.revokedAt());
                        jpaRepository.save(entity);
                    });
        } else {
            jpaRepository.save(toEntity(refreshToken));
        }
    }

    @Override
    public Optional<RefreshToken> findByHashedValue(String hashedValue) {
        return jpaRepository.findByHashedValue(hashedValue).map(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> findById(TokenId tokenId) {
        return jpaRepository.findById(tokenId.value()).map(this::toDomain);
    }

    @Override
    public List<RefreshToken> findByFamily(TokenFamily family) {
        return jpaRepository.findByFamilyId(family.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void revokeFamily(TokenFamily family) {
        jpaRepository.revokeByFamilyId(family.value(), Instant.now());
    }

    @Override
    public void revokeAllForUser(UserId userId) {
        jpaRepository.revokeByUserId(userId.value(), Instant.now());
    }

    @Override
    public void deleteExpired() {
        jpaRepository.deleteExpiredBefore(Instant.now());
    }

    @Override
    public List<RefreshToken> findActiveForUser(UserId userId) {
        return jpaRepository.findByUserIdAndRevokedFalse(userId.value())
                .stream().map(this::toDomain).toList();
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return RefreshTokenJpaEntity.of(
                token.tokenId().value(),
                token.family().value(),
                token.userId().value(),
                token.hashedValue(),
                token.issuedAt(),
                token.expiresAt(),
                token.isRevoked(),
                token.revokedAt()
        );
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.reconstitute(
                TokenId.of(entity.getTokenId().toString()),
                TokenFamily.of(entity.getFamilyId().toString()),
                UserId.of(entity.getUserId().toString()),
                entity.getHashedValue(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.isRevoked(),
                entity.getRevokedAt()
        );
    }
}
