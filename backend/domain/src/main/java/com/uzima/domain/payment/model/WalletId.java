package com.uzima.domain.payment.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'un portefeuille (Wallet).
 */
public record WalletId(UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "L'identifiant du portefeuille ne peut pas être nul");
    }

    public static WalletId generate() {
        return new WalletId(UUID.randomUUID());
    }

    public static WalletId of(UUID value) {
        return new WalletId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
