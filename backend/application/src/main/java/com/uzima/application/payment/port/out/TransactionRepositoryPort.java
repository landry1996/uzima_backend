package com.uzima.application.payment.port.out;

import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie (application) : persistance des transactions.
 * Défini dans la couche application, implémenté dans l'infrastructure.
 * Miroir du domain port {@code TransactionRepository}, séparé
 * pour respecter la règle de dépendance Application → Domain.
 */
public interface TransactionRepositoryPort {

    void save(Transaction transaction);

    Optional<Transaction> findById(TransactionId id);

    List<Transaction> findBySenderId(UserId senderId, int limit, int offset);

    List<Transaction> findByRecipientId(UserId recipientId, int limit, int offset);

    long countBySenderId(UserId senderId);

    long countByRecipientId(UserId recipientId);
}
