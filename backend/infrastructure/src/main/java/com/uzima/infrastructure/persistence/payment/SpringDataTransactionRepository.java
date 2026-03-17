package com.uzima.infrastructure.persistence.payment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository Spring Data JPA interne : transactions.
 * Package-private : ne doit pas être utilisé directement hors de l'infrastructure.
 */
public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    List<TransactionJpaEntity> findBySenderIdOrderByInitiatedAtDesc(UUID senderId, Pageable pageable);

    List<TransactionJpaEntity> findByRecipientIdOrderByInitiatedAtDesc(UUID recipientId, Pageable pageable);

    long countBySenderId(UUID senderId);

    long countByRecipientId(UUID recipientId);
}
