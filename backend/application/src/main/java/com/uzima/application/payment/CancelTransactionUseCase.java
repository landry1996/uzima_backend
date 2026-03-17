package com.uzima.application.payment;

import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Use Case : Annulation d'une transaction par son expéditeur.
 * Seul l'expéditeur peut annuler sa propre transaction.
 * L'annulation n'est possible que si la transaction est encore PENDING.
 */
public final class CancelTransactionUseCase {

    private final TransactionRepositoryPort transactionRepository;
    private final TimeProvider clock;

    public CancelTransactionUseCase(TransactionRepositoryPort transactionRepository, TimeProvider clock) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * @throws ResourceNotFoundException  si la transaction n'existe pas
     * @throws TransactionAccessDeniedException si l'utilisateur n'est pas l'expéditeur
     * @throws Transaction.IllegalTransitionException si la transaction n'est pas annulable
     */
    public void execute(TransactionId transactionId, UserId requesterId) {
        Objects.requireNonNull(transactionId, "L'identifiant de transaction est obligatoire");
        Objects.requireNonNull(requesterId,   "L'identifiant du demandeur est obligatoire");

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ResourceNotFoundException.transactionNotFound(transactionId));

        if (!transaction.senderId().equals(requesterId)) {
            throw new TransactionAccessDeniedException(
                "Seul l'expéditeur peut annuler la transaction " + transactionId
            );
        }

        transaction.cancel(clock);
        transactionRepository.save(transaction);
    }

    /** L'utilisateur n'est pas l'expéditeur de la transaction. HTTP 403. */
    public static final class TransactionAccessDeniedException extends UnauthorizedException {
        public TransactionAccessDeniedException(String message) {
            super("TRANSACTION_ACCESS_DENIED", message);
        }
    }
}
