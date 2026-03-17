package com.uzima.infrastructure.persistence.payment;

import com.uzima.domain.payment.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'wallets'.
 * Infrastructure uniquement. Pas de logique métier.
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallets_owner_id", columnList = "owner_id", unique = true)
})
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WalletJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid", unique = true)
    private UUID ownerId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static WalletJpaEntity of(
            UUID id,
            UUID ownerId,
            BigDecimal balance,
            Currency currency,
            Instant createdAt,
            Instant updatedAt
    ) {
        WalletJpaEntity e = new WalletJpaEntity();
        e.id        = id;
        e.ownerId   = ownerId;
        e.balance   = balance;
        e.currency  = currency;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WalletJpaEntity w)) return false;
        return id != null && id.equals(w.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
