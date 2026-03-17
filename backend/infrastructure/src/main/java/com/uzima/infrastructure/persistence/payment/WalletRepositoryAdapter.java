package com.uzima.infrastructure.persistence.payment;

import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Wallet;
import com.uzima.domain.payment.model.WalletId;
import com.uzima.domain.payment.port.WalletRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.infrastructure.shared.exception.DatabaseException;

import java.util.Optional;

/**
 * Adaptateur JPA : implémentation de {@link WalletRepositoryPort}.
 */
public class WalletRepositoryAdapter implements WalletRepositoryPort {

    private final SpringDataWalletRepository jpa;

    public WalletRepositoryAdapter(SpringDataWalletRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Wallet> findByOwnerId(UserId userId) {
        return jpa.findByOwnerId(userId.value())
                .map(this::toDomain);
    }

    @Override
    public void save(Wallet wallet) {
        try {
            jpa.save(toEntity(wallet));
        } catch (Exception ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde du portefeuille " + wallet.id(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private Wallet toDomain(WalletJpaEntity e) {
        return Wallet.reconstitute(
            WalletId.of(e.getId()),
            UserId.of(e.getOwnerId()),
            Money.of(e.getBalance(), e.getCurrency()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private WalletJpaEntity toEntity(Wallet w) {
        return WalletJpaEntity.of(
            w.id().value(),
            w.ownerId().value(),
            w.balance().amount(),
            w.balance().currency(),
            w.createdAt(),
            w.updatedAt()
        );
    }
}
