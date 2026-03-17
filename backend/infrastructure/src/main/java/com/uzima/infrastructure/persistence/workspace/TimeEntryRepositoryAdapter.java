package com.uzima.infrastructure.persistence.workspace;

import com.uzima.application.workspace.port.out.TimeEntryRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.workspace.model.TimeEntryId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente TimeEntryRepositoryPort avec JPA.
 */
public final class TimeEntryRepositoryAdapter implements TimeEntryRepositoryPort {

    private final SpringDataTimeEntryRepository jpaRepository;

    public TimeEntryRepositoryAdapter(SpringDataTimeEntryRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(TimeEntry timeEntry) {
        try {
            jpaRepository.save(ProjectEntityMapper.toJpaEntity(timeEntry));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde de l'entrée de temps", ex);
        }
    }

    @Override
    public Optional<TimeEntry> findById(TimeEntryId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(ProjectEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de l'entrée de temps", ex);
        }
    }

    @Override
    public List<TimeEntry> findByProjectId(ProjectId projectId) {
        try {
            return jpaRepository.findByProjectId(projectId.value()).stream()
                    .map(ProjectEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des entrées par projet", ex);
        }
    }

    @Override
    public List<TimeEntry> findByUserId(UserId userId) {
        try {
            return jpaRepository.findByUserId(userId.value()).stream()
                    .map(ProjectEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des entrées par utilisateur", ex);
        }
    }

    @Override
    public Optional<TimeEntry> findRunningForUser(UserId userId) {
        try {
            return jpaRepository.findRunningForUser(userId.value())
                    .map(ProjectEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de l'entrée en cours", ex);
        }
    }
}
