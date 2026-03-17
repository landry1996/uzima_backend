package com.uzima.infrastructure.persistence.workspace;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskId;

/**
 * Mapper : Conversion bidirectionnelle entre Task (domaine) et TaskJpaEntity (infra).
 *
 * Task.reconstitute() signature :
 *   (id, title, projectId, assigneeId, status, priority, description, blockedReason,
 *    createdAt, updatedAt, completedAt)
 * Note : description n'est pas persisté séparément (toujours null à la création).
 */
public final class TaskEntityMapper {

    private TaskEntityMapper() {}

    // -------------------------------------------------------------------------
    // Domaine → JPA
    // -------------------------------------------------------------------------

    public static TaskJpaEntity toJpaEntity(Task task) {
        return TaskJpaEntity.of(
            task.id().value(),
            task.title(),
            task.projectId().value(),
            task.assigneeId().map(UserId::value).orElse(null),
            task.status(),
            task.priority(),
            task.blockedReason().orElse(null),
            task.createdAt(),
            task.updatedAt(),                   // Instant, never null
            task.completedAt().orElse(null)
        );
    }

    // -------------------------------------------------------------------------
    // JPA → Domaine
    // -------------------------------------------------------------------------

    public static Task toDomain(TaskJpaEntity entity) {
        return Task.reconstitute(
            TaskId.of(entity.getId()),
            entity.getTitle(),
            ProjectId.of(entity.getProjectId()),
            entity.getAssigneeId() != null ? UserId.of(entity.getAssigneeId()) : null,
            entity.getStatus(),
            entity.getPriority(),
            null,                               // description non persisté
            entity.getBlockedReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCompletedAt()
        );
    }
}
