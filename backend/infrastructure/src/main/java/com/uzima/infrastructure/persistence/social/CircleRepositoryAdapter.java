package com.uzima.infrastructure.persistence.social;

import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.user.model.UserId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente CircleRepositoryPort avec JPA.
 * Délègue le mapping à CircleEntityMapper.
 * Enveloppe les exceptions JPA en DatabaseException.
 */
public final class CircleRepositoryAdapter implements CircleRepositoryPort {

    private final SpringDataCircleRepository jpaRepository;

    public CircleRepositoryAdapter(SpringDataCircleRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(Circle circle) {
        try {
            jpaRepository.save(CircleEntityMapper.toJpaEntity(circle));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde du cercle", ex);
        }
    }

    @Override
    public Optional<Circle> findById(CircleId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(CircleEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche du cercle", ex);
        }
    }

    @Override
    public List<Circle> findByMemberId(UserId memberId) {
        try {
            return jpaRepository.findByMemberId(memberId.value()).stream()
                    .map(CircleEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des cercles par membre", ex);
        }
    }

    @Override
    public List<Circle> findByOwnerId(UserId ownerId) {
        try {
            return jpaRepository.findByOwnerId(ownerId.value()).stream()
                    .map(CircleEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des cercles par propriétaire", ex);
        }
    }
}
