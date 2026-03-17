package com.uzima.infrastructure.persistence.workspace;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.TaskId;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.workspace.model.TimeEntryId;

import java.util.List;
import java.util.UUID;

/**
 * Mapper : Conversion bidirectionnelle entre Project (domaine) et ProjectJpaEntity (infra).
 *
 * La reconstitution requiert les taskIds et timeEntries chargés séparément
 * (Task et TimeEntry ont leurs propres tables avec FK project_id).
 */
public final class ProjectEntityMapper {

    private ProjectEntityMapper() {}

    // -------------------------------------------------------------------------
    // Domaine → JPA
    // -------------------------------------------------------------------------

    public static ProjectJpaEntity toJpaEntity(Project project) {
        List<ProjectMemberJpaEntity> memberEntities = project.members().stream()
                .map(m -> ProjectMemberJpaEntity.of(
                    UUID.randomUUID(),
                    project.id().value(),
                    m.userId().value(),
                    m.role(),
                    m.joinedAt()
                ))
                .toList();

        return ProjectJpaEntity.of(
            project.id().value(),
            project.name(),
            project.ownerId().value(),
            project.createdAt(),
            memberEntities
        );
    }

    public static TimeEntryJpaEntity toJpaEntity(TimeEntry entry) {
        return TimeEntryJpaEntity.of(
            entry.id().value(),
            entry.userId().value(),
            entry.projectId().value(),
            entry.description().orElse(null),
            entry.startedAt(),
            entry.stoppedAt().orElse(null)
        );
    }

    // -------------------------------------------------------------------------
    // JPA → Domaine
    // -------------------------------------------------------------------------

    public static Project toDomain(ProjectJpaEntity entity,
                                    List<TaskId> taskIds,
                                    List<TimeEntry> timeEntries) {
        List<Project.ProjectMember> members = entity.getMembers().stream()
                .map(m -> new Project.ProjectMember(
                    UserId.of(m.getUserId()),
                    m.getRole(),
                    m.getJoinedAt()
                ))
                .toList();

        return Project.reconstitute(
            ProjectId.of(entity.getId()),
            entity.getName(),
            UserId.of(entity.getOwnerId()),
            entity.getCreatedAt(),
            members,
            taskIds,
            timeEntries
        );
    }

    public static TimeEntry toDomain(TimeEntryJpaEntity entity) {
        return TimeEntry.reconstitute(
            TimeEntryId.of(entity.getId()),
            UserId.of(entity.getUserId()),
            ProjectId.of(entity.getProjectId()),
            entity.getDescription(),
            entity.getStartedAt(),
            entity.getStoppedAt()
        );
    }
}
