package com.uzima.infrastructure.persistence.workspace;

import com.uzima.domain.workspace.model.TaskPriority;
import com.uzima.domain.workspace.model.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'tasks'.
 * Task est un Aggregate Root indépendant — table propre, FK project_id.
 */
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_tasks_project_id",  columnList = "project_id"),
        @Index(name = "idx_tasks_assignee_id", columnList = "assignee_id"),
        @Index(name = "idx_tasks_status",      columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TaskJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "assignee_id", columnDefinition = "uuid")
    private UUID assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static TaskJpaEntity of(
            UUID id, String title, UUID projectId, UUID assigneeId,
            TaskStatus status, TaskPriority priority, String blockedReason,
            Instant createdAt, Instant updatedAt, Instant completedAt
    ) {
        TaskJpaEntity e = new TaskJpaEntity();
        e.id           = id;
        e.title        = title;
        e.projectId    = projectId;
        e.assigneeId   = assigneeId;
        e.status       = status;
        e.priority     = priority;
        e.blockedReason = blockedReason;
        e.createdAt    = createdAt;
        e.updatedAt    = updatedAt;
        e.completedAt  = completedAt;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskJpaEntity t)) return false;
        return id != null && id.equals(t.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
