package com.uzima.infrastructure.persistence.workspace;

import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskId;
import com.uzima.domain.workspace.model.TaskStatus;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente TaskRepositoryPort avec JPA.
 */
public final class TaskRepositoryAdapter implements TaskRepositoryPort {

    private final SpringDataTaskRepository jpaRepository;

    public TaskRepositoryAdapter(SpringDataTaskRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(Task task) {
        try {
            jpaRepository.save(TaskEntityMapper.toJpaEntity(task));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde de la tâche", ex);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(TaskEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de la tâche", ex);
        }
    }

    @Override
    public List<Task> findByProjectId(ProjectId projectId) {
        try {
            return jpaRepository.findByProjectId(projectId.value()).stream()
                    .map(TaskEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des tâches du projet", ex);
        }
    }

    @Override
    public List<Task> findByProjectIdAndStatus(ProjectId projectId, TaskStatus status) {
        try {
            return jpaRepository.findByProjectIdAndStatus(projectId.value(), status).stream()
                    .map(TaskEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des tâches par statut", ex);
        }
    }

    @Override
    public List<Task> findByAssigneeId(UserId assigneeId) {
        try {
            return jpaRepository.findByAssigneeId(assigneeId.value()).stream()
                    .map(TaskEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des tâches par assigné", ex);
        }
    }
}
