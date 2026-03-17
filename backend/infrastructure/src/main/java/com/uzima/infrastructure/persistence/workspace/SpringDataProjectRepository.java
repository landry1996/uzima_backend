package com.uzima.infrastructure.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository pour les projets. */
public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, UUID> {

    List<ProjectJpaEntity> findByOwnerId(UUID ownerId);

    @Query("SELECT DISTINCT p FROM ProjectJpaEntity p JOIN p.members m WHERE m.userId = :userId")
    List<ProjectJpaEntity> findByMemberId(@Param("userId") UUID userId);
}
