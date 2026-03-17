package com.uzima.application.invoice.port.in;

import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Annulation d'une facture. */
public record CancelInvoiceCommand(InvoiceId invoiceId, UserId requesterId) {

    public CancelInvoiceCommand {
        Objects.requireNonNull(invoiceId,   "L'identifiant de la facture est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
    }
}
