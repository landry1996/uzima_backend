package com.uzima.application.payment;

import com.uzima.application.payment.port.in.SendPaymentCommand;
import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Envoi d'un paiement entre deux utilisateurs.
 * Orchestration :
 * 1. Créer la transaction en état PENDING via Transaction.initiate()
 * 2. Persister en PENDING (rollback possible si gateway plante)
 * 3. Appeler la gateway externe
 * 4. complete() ou fail() selon la réponse
 * 5. Persister l'état final
 * 6. Retourner l'identifiant de la transaction
 * La logique métier (sender ≠ recipient, amount > 0) reste dans le domaine.
 * Ce use case n'orchestre que la séquence.
 */
public final class SendPaymentUseCase {

    private final TransactionRepositoryPort transactionRepository;
    private final PaymentGatewayPort paymentGateway;
    private final TimeProvider clock;

    public SendPaymentUseCase(
            TransactionRepositoryPort transactionRepository,
            PaymentGatewayPort paymentGateway,
            TimeProvider clock
    ) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "Le repository de transactions est obligatoire");
        this.paymentGateway        = Objects.requireNonNull(paymentGateway, "La gateway de paiement est obligatoire");
        this.clock                 = Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
    }

    /**
     * @return L'identifiant de la transaction créée (COMPLETED ou FAILED)
     * @throws Transaction.SelfPaymentException       si sender == recipient
     * @throws Transaction.NonPositiveAmountException si montant <= 0
     * @throws Money.CurrencyMismatchException        si mismatch de devise (cas rare)
     */
    public TransactionId execute(SendPaymentCommand command) {
        Objects.requireNonNull(command, "La commande de paiement est obligatoire");

        // 1. Créer la transaction en PENDING — les invariants sont protégés ici
        Money amount = Money.of(command.amount(), command.currency());
        Transaction transaction = Transaction.initiate(
            command.senderId(),
            command.recipientId(),
            amount,
            command.method(),
            command.description(),
            clock
        );

        // 2. Persister en PENDING avant d'appeler la gateway
        //    → permet de tracer même si la gateway est indisponible
        transactionRepository.save(transaction);

        // 3. Appeler la gateway externe
        PaymentGatewayPort.GatewayResponse response;
        try {
            response = paymentGateway.process(transaction);
        } catch (Exception ex) {
            // Gateway inaccessible → echec technique
            transaction.fail("Gateway indisponible : " + ex.getMessage(), clock);
            transactionRepository.save(transaction);
            throw new PaymentGatewayException("La gateway de paiement est temporairement indisponible", ex);
        }

        // 4. Mettre à jour l'état selon la réponse
        if (response.success()) {
            transaction.complete(response.externalId(), clock);
        } else {
            transaction.fail(response.errorMessage(), clock);
        }

        // 5. Persister l'état final
        transactionRepository.save(transaction);

        return transaction.id();
    }

    // -------------------------------------------------------------------------
    // Exceptions applicatives
    // -------------------------------------------------------------------------

    /** Gateway inaccessible (erreur réseau ou timeout). HTTP 502. */
    public static final class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
