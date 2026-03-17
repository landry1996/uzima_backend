package com.uzima.domain.invoice.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'une ligne de facture. */
public record InvoiceItemId(UUID value) {

    public InvoiceItemId {
        Objects.requireNonNull(value, "L'identifiant de ligne de facture ne peut pas être nul");
    }

    public static InvoiceItemId generate() { return new InvoiceItemId(UUID.randomUUID()); }
    public static InvoiceItemId of(UUID uuid) { return new InvoiceItemId(uuid); }

    @Override
    public String toString() { return value.toString(); }
}
