package com.uzima.infrastructure.persistence.workspace;

import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.TaskId;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente ProjectRepositoryPort avec JPA.
 * <p>
 * La reconstitution d'un Project requiert les TaskId et TimeEntry
 * chargés depuis leurs tables respectives (FK project_id).
 */
public final class ProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final SpringDataProjectRepository   projectJpa;
    private final SpringDataTaskRepository      taskJpa;
    private final SpringDataTimeEntryRepository timeEntryJpa;

    public ProjectRepositoryAdapter(
            SpringDataProjectRepository projectJpa,
            SpringDataTaskRepository taskJpa,
            SpringDataTimeEntryRepository timeEntryJpa
    ) {
        this.projectJpa   = Objects.requireNonNull(projectJpa,   "Le repository de projets est obligatoire");
        this.taskJpa      = Objects.requireNonNull(taskJpa,      "Le repository de tâches est obligatoire");
        this.timeEntryJpa = Objects.requireNonNull(timeEntryJpa, "Le repository d'entrées de temps est obligatoire");
    }

    @Override
    public void save(Project project) {
        try {
            projectJpa.save(ProjectEntityMapper.toJpaEntity(project));
            // Les TimeEntry liées au projet sont sauvegardées par TrackTimeUseCase
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde du projet", ex);
        }
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        try {
            return projectJpa.findById(id.value())
                    .map(this::reconstitute);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche du projet", ex);
        }
    }

    @Override
    public List<Project> findByMemberId(UserId userId) {
        try {
            return projectJpa.findByMemberId(userId.value()).stream()
                    .map(this::reconstitute)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des projets par membre", ex);
        }
    }

    @Override
    public List<Project> findByOwnerId(UserId userId) {
        try {
            return projectJpa.findByOwnerId(userId.value()).stream()
                    .map(this::reconstitute)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des projets par propriétaire", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Helper privé
    // -------------------------------------------------------------------------

    private Project reconstitute(ProjectJpaEntity entity) {
        List<TaskId> taskIds = taskJpa.findByProjectId(entity.getId()).stream()
                .map(t -> TaskId.of(t.getId()))
                .toList();

        List<TimeEntry> timeEntries = timeEntryJpa.findByProjectId(entity.getId()).stream()
                .map(ProjectEntityMapper::toDomain)
                .toList();

        return ProjectEntityMapper.toDomain(entity, taskIds, timeEntries);
    }
}
