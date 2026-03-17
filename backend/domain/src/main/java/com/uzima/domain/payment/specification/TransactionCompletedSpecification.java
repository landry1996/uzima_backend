package com.uzima.domain.payment.specification;

import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.shared.specification.Specification;

/**
 * Specification : Vérifie qu'une transaction est dans l'état COMPLETED.
 * Utilisée pour filtrer les transactions réussies dans l'historique
 * ou pour valider des préconditions (ex: remboursement uniquement sur transactions complétées).
 */
public final class TransactionCompletedSpecification implements Specification<Transaction> {

    @Override
    public boolean isSatisfiedBy(Transaction transaction) {
        return transaction.isCompleted();
    }
}
