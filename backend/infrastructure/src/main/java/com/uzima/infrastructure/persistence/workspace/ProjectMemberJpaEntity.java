package com.uzima.infrastructure.persistence.workspace;

import com.uzima.domain.workspace.model.ProjectRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'project_members'.
 * Appartient à l'agrégat Project (cascade ALL depuis ProjectJpaEntity).
 */
@Entity
@Table(
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_project_members_project_user",
        columnNames = {"project_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ProjectMemberJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public static ProjectMemberJpaEntity of(UUID id, UUID projectId, UUID userId,
                                             ProjectRole role, Instant joinedAt) {
        ProjectMemberJpaEntity e = new ProjectMemberJpaEntity();
        e.id        = id;
        e.projectId = projectId;
        e.userId    = userId;
        e.role      = role;
        e.joinedAt  = joinedAt;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMemberJpaEntity m)) return false;
        return id != null && id.equals(m.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
