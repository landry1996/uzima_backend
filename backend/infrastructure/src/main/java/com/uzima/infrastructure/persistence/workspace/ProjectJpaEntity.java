package com.uzima.infrastructure.persistence.workspace;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA : Table 'projects'.
 * Les membres sont gérés en cascade (aggregate boundary).
 * Les TaskId et TimeEntry sont gérés via leurs propres tables (projectId FK).
 */
@Entity
@Table(
    name = "projects",
    indexes = {
        @Index(name = "idx_projects_owner_id",   columnList = "owner_id"),
        @Index(name = "idx_projects_created_at", columnList = "created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ProjectJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private List<ProjectMemberJpaEntity> members = new ArrayList<>();

    public static ProjectJpaEntity of(UUID id, String name, UUID ownerId,
                                       Instant createdAt, List<ProjectMemberJpaEntity> members) {
        ProjectJpaEntity e = new ProjectJpaEntity();
        e.id        = id;
        e.name      = name;
        e.ownerId   = ownerId;
        e.createdAt = createdAt;
        e.members.addAll(members);
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectJpaEntity p)) return false;
        return id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
