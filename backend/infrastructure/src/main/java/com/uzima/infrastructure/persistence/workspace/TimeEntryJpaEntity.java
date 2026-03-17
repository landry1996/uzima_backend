package com.uzima.infrastructure.persistence.workspace;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'time_entries'.
 * TimeEntry est une entité appartenant au projet.
 */
@Entity
@Table(
    name = "time_entries",
    indexes = {
        @Index(name = "idx_time_entries_project_id", columnList = "project_id"),
        @Index(name = "idx_time_entries_user_id",    columnList = "user_id"),
        @Index(name = "idx_time_entries_started_at", columnList = "started_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TimeEntryJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    public static TimeEntryJpaEntity of(UUID id, UUID userId, UUID projectId,
                                         String description, Instant startedAt, Instant stoppedAt) {
        TimeEntryJpaEntity e = new TimeEntryJpaEntity();
        e.id          = id;
        e.userId      = userId;
        e.projectId   = projectId;
        e.description = description;
        e.startedAt   = startedAt;
        e.stoppedAt   = stoppedAt;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeEntryJpaEntity t)) return false;
        return id != null && id.equals(t.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
