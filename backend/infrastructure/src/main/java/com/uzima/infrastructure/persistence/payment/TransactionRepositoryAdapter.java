package com.uzima.infrastructure.persistence.payment;

import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.user.model.UserId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente TransactionRepositoryPort avec JPA.
 * Délègue le mapping à TransactionEntityMapper.
 * Enveloppe les exceptions JPA en DatabaseException.
 */
public final class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final SpringDataTransactionRepository jpaRepository;

    public TransactionRepositoryAdapter(SpringDataTransactionRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(Transaction transaction) {
        try {
            jpaRepository.save(TransactionEntityMapper.toJpaEntity(transaction));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde de la transaction", ex);
        }
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(TransactionEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de la transaction", ex);
        }
    }

    @Override
    public List<Transaction> findBySenderId(UserId senderId, int limit, int offset) {
        try {
            int page = offset / Math.max(limit, 1);
            return jpaRepository
                    .findBySenderIdOrderByInitiatedAtDesc(senderId.value(), PageRequest.of(page, limit))
                    .stream()
                    .map(TransactionEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des transactions envoyées", ex);
        }
    }

    @Override
    public List<Transaction> findByRecipientId(UserId recipientId, int limit, int offset) {
        try {
            int page = offset / Math.max(limit, 1);
            return jpaRepository
                    .findByRecipientIdOrderByInitiatedAtDesc(recipientId.value(), PageRequest.of(page, limit))
                    .stream()
                    .map(TransactionEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des transactions reçues", ex);
        }
    }

    @Override
    public long countBySenderId(UserId senderId) {
        try {
            return jpaRepository.countBySenderId(senderId.value());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors du comptage des transactions", ex);
        }
    }

    @Override
    public long countByRecipientId(UserId recipientId) {
        try {
            return jpaRepository.countByRecipientId(recipientId.value());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors du comptage des transactions reçues", ex);
        }
    }
}
