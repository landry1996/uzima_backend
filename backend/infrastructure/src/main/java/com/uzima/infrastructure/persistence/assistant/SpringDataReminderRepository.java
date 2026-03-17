package com.uzima.infrastructure.persistence.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataReminderRepository extends JpaRepository<ReminderJpaEntity, UUID> {

    List<ReminderJpaEntity> findByUserId(UUID userId);

    List<ReminderJpaEntity> findByUserIdAndStatus(UUID userId, String status);

    /** Rappels actifs : statut PENDING ou SNOOZED. */
    List<ReminderJpaEntity> findByUserIdAndStatusIn(UUID userId, List<String> statuses);
}
