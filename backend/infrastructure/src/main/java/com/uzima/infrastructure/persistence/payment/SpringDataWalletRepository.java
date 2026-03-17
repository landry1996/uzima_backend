package com.uzima.infrastructure.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les portefeuilles.
 */
public interface SpringDataWalletRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByOwnerId(UUID ownerId);
}
