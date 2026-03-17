package com.uzima.bootstrap.adapter.http.payment;

import com.uzima.domain.payment.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO HTTP sortant : Détail d'une transaction.
 */
public record PaymentResponse(
        String transactionId,
        String senderId,
        String recipientId,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String description,
        String externalId,
        String failureReason,
        Instant initiatedAt,
        Instant completedAt,
        Instant failedAt,
        Instant cancelledAt
) {
    public static PaymentResponse from(Transaction tx) {
        return new PaymentResponse(
            tx.id().toString(),
            tx.senderId().toString(),
            tx.recipientId().toString(),
            tx.amount().amount(),
            tx.amount().currency().name(),
            tx.method().name(),
            tx.status().name(),
            tx.description().orElse(null),
            tx.externalId().orElse(null),
            tx.failureReason().orElse(null),
            tx.initiatedAt(),
            tx.completedAt().orElse(null),
            tx.failedAt().orElse(null),
            tx.cancelledAt().orElse(null)
        );
    }
}
