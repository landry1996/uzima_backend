package com.uzima.infrastructure.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository pour les entrées de temps. */
public interface SpringDataTimeEntryRepository extends JpaRepository<TimeEntryJpaEntity, UUID> {

    List<TimeEntryJpaEntity> findByProjectId(UUID projectId);

    List<TimeEntryJpaEntity> findByUserId(UUID userId);

    @Query("SELECT t FROM TimeEntryJpaEntity t WHERE t.userId = :userId AND t.stoppedAt IS NULL")
    Optional<TimeEntryJpaEntity> findRunningForUser(@Param("userId") UUID userId);
}
