package com.uzima.infrastructure.persistence.workspace;

import com.uzima.domain.workspace.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository pour les tâches. */
public interface SpringDataTaskRepository extends JpaRepository<TaskJpaEntity, UUID> {

    List<TaskJpaEntity> findByProjectId(UUID projectId);

    List<TaskJpaEntity> findByProjectIdAndStatus(UUID projectId, TaskStatus status);

    List<TaskJpaEntity> findByAssigneeId(UUID assigneeId);
}
