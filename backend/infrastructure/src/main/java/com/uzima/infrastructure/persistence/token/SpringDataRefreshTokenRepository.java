package com.uzima.infrastructure.persistence.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByHashedValue(String hashedValue);

    List<RefreshTokenJpaEntity> findByFamilyId(UUID familyId);

    List<RefreshTokenJpaEntity> findByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true, t.revokedAt = :revokedAt WHERE t.familyId = :familyId AND t.revoked = false")
    void revokeByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true, t.revokedAt = :revokedAt WHERE t.userId = :userId AND t.revoked = false")
    void revokeByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenJpaEntity t WHERE t.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") Instant now);
}
