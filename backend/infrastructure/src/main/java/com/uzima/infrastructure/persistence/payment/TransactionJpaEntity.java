package com.uzima.infrastructure.persistence.payment;

import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'transactions'.
 * Infrastructure uniquement. Pas de logique métier.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_sender_id",    columnList = "sender_id"),
    @Index(name = "idx_transactions_recipient_id", columnList = "recipient_id"),
    @Index(name = "idx_transactions_status",       columnList = "status"),
    @Index(name = "idx_transactions_initiated_at", columnList = "initiated_at DESC")
})
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TransactionJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "sender_id", nullable = false, columnDefinition = "uuid")
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false, columnDefinition = "uuid")
    private UUID recipientId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public static TransactionJpaEntity of(
            UUID id,
            UUID senderId,
            UUID recipientId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            TransactionStatus status,
            String description,
            String externalId,
            String failureReason,
            Instant initiatedAt,
            Instant completedAt,
            Instant failedAt,
            Instant cancelledAt
    ) {
        TransactionJpaEntity e = new TransactionJpaEntity();
        e.id            = id;
        e.senderId      = senderId;
        e.recipientId   = recipientId;
        e.amount        = amount;
        e.currency      = currency;
        e.method        = method;
        e.status        = status;
        e.description   = description;
        e.externalId    = externalId;
        e.failureReason = failureReason;
        e.initiatedAt   = initiatedAt;
        e.completedAt   = completedAt;
        e.failedAt      = failedAt;
        e.cancelledAt   = cancelledAt;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionJpaEntity t)) return false;
        return id != null && id.equals(t.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
