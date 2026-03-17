package com.uzima.infrastructure.persistence.user;

import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.user.model.PhoneNumber;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente UserRepositoryPort avec JPA.
 * <p>
 * Responsabilités :
 * - Coordonner les opérations CRUD (orchestration)
 * - Déléguer le mapping à UserEntityMapper (transformation)
 * - Envelopper les exceptions JPA en DatabaseException (isolation technique)
 * <p>
 * La logique de mapping est extraite dans UserEntityMapper (SRP).
 * Les exceptions JPA (DataAccessException) sont capturées ici et
 * converties en DatabaseException → ne remontent jamais vers le domaine.
 */
public final class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository jpaRepository;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(User user) {
        try {
            jpaRepository.save(UserEntityMapper.toJpaEntity(user));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde de l'utilisateur", ex);
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(UserEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de l'utilisateur par ID", ex);
        }
    }

    @Override
    public Optional<User> findByPhoneNumber(PhoneNumber phoneNumber) {
        try {
            return jpaRepository.findByPhoneNumber(phoneNumber.value())
                    .map(UserEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de l'utilisateur par téléphone", ex);
        }
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        try {
            return jpaRepository.existsByPhoneNumber(phoneNumber.value());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la vérification du numéro de téléphone", ex);
        }
    }
}
