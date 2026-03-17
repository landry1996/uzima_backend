package com.uzima.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA : interface technique interne à l'infrastructure.
 * <p>
 * Cette interface n'est PAS visible du domaine ni de l'application.
 * Elle est utilisée exclusivement par UserRepositoryAdapter.
 */
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
