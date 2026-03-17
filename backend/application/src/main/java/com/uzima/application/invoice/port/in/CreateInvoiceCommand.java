package com.uzima.application.invoice.port.in;

import com.uzima.domain.invoice.model.TaxRate;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.user.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Commande : Création d'une facture avec ses lignes. */
public record CreateInvoiceCommand(
        UserId     issuerId,
        UserId     clientId,
        LocalDate  dueDate,
        List<Item> items
) {
    public CreateInvoiceCommand {
        Objects.requireNonNull(issuerId, "L'émetteur est obligatoire");
        Objects.requireNonNull(clientId, "Le client est obligatoire");
        Objects.requireNonNull(dueDate,  "La date d'échéance est obligatoire");
        Objects.requireNonNull(items,    "La liste des lignes est obligatoire");
    }

    /** Représentation d'une ligne de facture dans la commande. */
    public record Item(
            String     description,
            int        quantity,
            BigDecimal unitAmount,
            Currency   currency,
            TaxRate    taxRate
    ) {
        public Item {
            Objects.requireNonNull(description, "La description est obligatoire");
            Objects.requireNonNull(unitAmount,  "Le montant unitaire est obligatoire");
            Objects.requireNonNull(currency,    "La devise est obligatoire");
            Objects.requireNonNull(taxRate,     "Le taux de TVA est obligatoire");
        }
    }
}
