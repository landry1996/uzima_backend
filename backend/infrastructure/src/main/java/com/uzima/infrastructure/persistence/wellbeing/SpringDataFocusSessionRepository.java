package com.uzima.infrastructure.persistence.wellbeing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataFocusSessionRepository extends JpaRepository<FocusSessionJpaEntity, UUID> {

    List<FocusSessionJpaEntity> findByUserId(UUID userId);

    List<FocusSessionJpaEntity> findByUserIdAndStatus(UUID userId, String status);

    List<FocusSessionJpaEntity> findByUserIdAndStartedAtBetween(UUID userId, Instant from, Instant to);

    Optional<FocusSessionJpaEntity> findFirstByUserIdAndStatus(UUID userId, String status);
}
