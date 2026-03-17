package com.uzima.application.payment;

import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Historique paginé des transactions d'un utilisateur.
 * Retourne à la fois les transactions envoyées ET reçues,
 * avec le total pour la pagination côté client.
 */
public final class GetTransactionHistoryUseCase {

    private final TransactionRepositoryPort transactionRepository;

    public GetTransactionHistoryUseCase(TransactionRepositoryPort transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "Le repository de transactions est obligatoire");
    }

    /**
     * Vue paginée de l'historique.
     *
     * @param sent     Transactions envoyées (tri décroissant par date)
     * @param received Transactions reçues (tri décroissant par date)
     * @param totalSent Nombre total de transactions envoyées
     * @param totalReceived Nombre total de transactions reçues
     */
    public record TransactionHistoryView(
            List<Transaction> sent,
            List<Transaction> received,
            long totalSent,
            long totalReceived
    ) {
        /** Nombre total de transactions (envoyées + reçues). */
        public long total() {
            return totalSent + totalReceived;
        }
    }

    /**
     * @param userId Identifiant de l'utilisateur
     * @param limit  Nombre de résultats par page
     * @param offset Décalage pour la pagination
     */
    public TransactionHistoryView execute(UserId userId, int limit, int offset) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        if (limit <= 0)  throw new IllegalArgumentException("La limite doit être > 0");
        if (offset < 0)  throw new IllegalArgumentException("L'offset ne peut pas être négatif");

        List<Transaction> sent     = transactionRepository.findBySenderId(userId, limit, offset);
        List<Transaction> received = transactionRepository.findByRecipientId(userId, limit, offset);
        long totalSent             = transactionRepository.countBySenderId(userId);
        long totalReceived         = transactionRepository.countByRecipientId(userId);

        return new TransactionHistoryView(sent, received, totalSent, totalReceived);
    }
}
