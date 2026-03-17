package com.uzima.application.payment.port.in;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.user.model.UserId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Commande d'entrée : Demande de paiement (request money).
 * L'initiateur (requesterId) demande à debtorId de lui payer amount.
 * La transaction est créée côté débiteur, pas encore exécutée.
 */
public record RequestPaymentCommand(
        UserId requesterId,
        UserId debtorId,
        BigDecimal amount,
        Currency currency,
        PaymentMethod preferredMethod,
        String reason
) {
    public RequestPaymentCommand {
        Objects.requireNonNull(requesterId,     "L'identifiant du créancier est obligatoire");
        Objects.requireNonNull(debtorId,        "L'identifiant du débiteur est obligatoire");
        Objects.requireNonNull(amount,          "Le montant est obligatoire");
        Objects.requireNonNull(currency,        "La devise est obligatoire");
        Objects.requireNonNull(preferredMethod, "La méthode de paiement est obligatoire");
    }
}
