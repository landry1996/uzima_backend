package com.uzima.domain.invoice.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'une facture. */
public record InvoiceId(UUID value) {

    public InvoiceId {
        Objects.requireNonNull(value, "L'identifiant de facture ne peut pas être nul");
    }

    public static InvoiceId generate() { return new InvoiceId(UUID.randomUUID()); }
    public static InvoiceId of(UUID uuid) { return new InvoiceId(uuid); }
    public static InvoiceId of(String uuid) {
        try { return new InvoiceId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) { throw new InvalidInvoiceIdException("Format UUID invalide : " + uuid); }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidInvoiceIdException extends RuntimeException {
        public InvalidInvoiceIdException(String message) { super(message); }
    }
}
