package com.uzima.application.payment.port.in;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.user.model.UserId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Commande d'entrée : Envoi d'un paiement.
 * Transportée du contrôleur HTTP au use case.
 * La validation métier (montant > 0, sender ≠ recipient) est déléguée à Transaction.initiate().
 */
public record SendPaymentCommand(
        UserId senderId,
        UserId recipientId,
        BigDecimal amount,
        Currency currency,
        PaymentMethod method,
        String description
) {
    public SendPaymentCommand {
        Objects.requireNonNull(senderId,    "L'identifiant de l'expéditeur est obligatoire");
        Objects.requireNonNull(recipientId, "L'identifiant du destinataire est obligatoire");
        Objects.requireNonNull(amount,      "Le montant est obligatoire");
        Objects.requireNonNull(currency,    "La devise est obligatoire");
        Objects.requireNonNull(method,      "La méthode de paiement est obligatoire");
        // description intentionnellement nullable
    }

}
