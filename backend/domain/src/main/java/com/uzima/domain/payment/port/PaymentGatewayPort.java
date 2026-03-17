package com.uzima.domain.payment.port;

import com.uzima.domain.payment.model.Transaction;

import java.util.Objects;

/**
 * Port OUT (sortie) : Gateway de paiement externe.
 * Abstraction du traitement réel (Mobile Money, Stripe, Wave, etc.).
 * Implémenté dans l'infrastructure (FakePaymentGatewayAdapter pour MVP,
 * OrangeMoneGatewayAdapter, StripeGatewayAdapter pour production).
 */
public interface PaymentGatewayPort {

    /**
     * Soumet une transaction à la gateway externe.
     *
     * @param transaction La transaction PENDING à traiter
     * @return Le résultat du traitement (succès ou échec avec raison)
     */
    GatewayResponse process(Transaction transaction);

    /**
     * Résultat retourné par la gateway.
     *
     * @param externalId   Identifiant de la transaction côté gateway (non nul si succès)
     * @param success      true si la transaction a été acceptée
     * @param errorMessage Message d'erreur si la transaction a été refusée
     */
    record GatewayResponse(String externalId, boolean success, String errorMessage) {

        /** Factory : résultat de succès. */
        public static GatewayResponse success(String externalId) {
            Objects.requireNonNull(externalId, "L'identifiant externe est obligatoire en cas de succès");
            return new GatewayResponse(externalId, true, null);
        }

        /** Factory : résultat d'échec. */
        public static GatewayResponse failure(String errorMessage) {
            Objects.requireNonNull(errorMessage, "Le message d'erreur est obligatoire en cas d'échec");
            return new GatewayResponse(null, false, errorMessage);
        }
    }
}
