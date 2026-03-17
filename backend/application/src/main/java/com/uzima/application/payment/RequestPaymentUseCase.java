package com.uzima.application.payment;

import com.uzima.application.payment.port.in.RequestPaymentCommand;
import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Demande de paiement (request money).
 * Le créancier (requester) crée une transaction PENDING en attente de paiement
 * par le débiteur. La transaction n'est pas exécutée ici — elle est mise en attente
 * jusqu'à ce que le débiteur confirme via SendPaymentUseCase.
 * Note : Pour le MVP, cette transaction PENDING est simplement enregistrée.
 * La notification au débiteur sera gérée par le domaine de messagerie (future intégration).
 */
public final class RequestPaymentUseCase {

    private final TransactionRepositoryPort transactionRepository;
    private final TimeProvider clock;

    public RequestPaymentUseCase(TransactionRepositoryPort transactionRepository, TimeProvider clock) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * @return L'identifiant de la transaction créée en PENDING
     */
    public TransactionId execute(RequestPaymentCommand command) {
        Objects.requireNonNull(command, "La commande de demande de paiement est obligatoire");

        Money amount = Money.of(command.amount(), command.currency());

        // Le débiteur est l'expéditeur futur, le créancier est le destinataire
        Transaction transaction = Transaction.initiate(
            command.debtorId(),
            command.requesterId(),
            amount,
            command.preferredMethod(),
            command.reason(),
            clock
        );

        transactionRepository.save(transaction);
        return transaction.id();
    }
}
