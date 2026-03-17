package com.uzima.domain.payment.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'une transaction.
 * Immuable. Deux TransactionId avec le même UUID sont égaux.
 */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "L'identifiant de transaction ne peut pas être nul");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(UUID uuid) {
        return new TransactionId(uuid);
    }

    public static TransactionId of(String uuid) {
        try {
            return new TransactionId(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            throw new InvalidTransactionIdException("Format UUID invalide : " + uuid);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static final class InvalidTransactionIdException extends RuntimeException {
        public InvalidTransactionIdException(String message) {
            super(message);
        }
    }
}
