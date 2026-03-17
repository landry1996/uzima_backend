package com.uzima.infrastructure.persistence.payment;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.user.model.UserId;

/**
 * Mapper : Conversion bidirectionnelle entre Transaction (domaine) et TransactionJpaEntity (infra).
 *
 * Utilise Transaction.reconstitute() pour l'hydratation — jamais de constructeur public.
 */
public final class TransactionEntityMapper {

    private TransactionEntityMapper() {}

    public static TransactionJpaEntity toJpaEntity(Transaction tx) {
        return TransactionJpaEntity.of(
            tx.id().value(),
            tx.senderId().value(),
            tx.recipientId().value(),
            tx.amount().amount(),
            tx.amount().currency().name(),
            tx.method(),
            tx.status(),
            tx.description().orElse(null),
            tx.externalId().orElse(null),
            tx.failureReason().orElse(null),
            tx.initiatedAt(),
            tx.completedAt().orElse(null),
            tx.failedAt().orElse(null),
            tx.cancelledAt().orElse(null)
        );
    }

    public static Transaction toDomain(TransactionJpaEntity entity) {
        Money amount = Money.of(entity.getAmount(), Currency.valueOf(entity.getCurrency()));
        return Transaction.reconstitute(
            TransactionId.of(entity.getId()),
            UserId.of(entity.getSenderId()),
            UserId.of(entity.getRecipientId()),
            amount,
            entity.getMethod(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getExternalId(),
            entity.getFailureReason(),
            entity.getInitiatedAt(),
            entity.getCompletedAt(),
            entity.getFailedAt(),
            entity.getCancelledAt()
        );
    }
}
