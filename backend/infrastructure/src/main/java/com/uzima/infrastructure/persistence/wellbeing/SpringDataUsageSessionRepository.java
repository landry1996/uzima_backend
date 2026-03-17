package com.uzima.infrastructure.persistence.wellbeing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataUsageSessionRepository extends JpaRepository<UsageSessionJpaEntity, UUID> {

    List<UsageSessionJpaEntity> findByUserId(UUID userId);

    List<UsageSessionJpaEntity> findByUserIdAndAppType(UUID userId, String appType);

    List<UsageSessionJpaEntity> findByUserIdAndStartedAtBetween(UUID userId, Instant from, Instant to);
}
