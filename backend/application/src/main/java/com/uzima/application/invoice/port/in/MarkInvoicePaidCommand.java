package com.uzima.application.invoice.port.in;

import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Marquage d'une facture comme payée (SENT → PAID). */
public record MarkInvoicePaidCommand(InvoiceId invoiceId, UserId requesterId) {

    public MarkInvoicePaidCommand {
        Objects.requireNonNull(invoiceId,   "L'identifiant de la facture est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
    }
}
