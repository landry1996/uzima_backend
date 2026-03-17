package com.uzima.domain.payment.specification;

import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.shared.specification.Specification;

import java.util.Objects;

/**
 * Specification : Vérifie qu'un solde disponible est suffisant pour couvrir
 * le montant d'une transaction.
 * Utilisée dans les use cases avant d'initier une transaction sur le
 * portefeuille interne Uzima (méthode WALLET).
 */
public final class SufficientFundsSpecification implements Specification<Transaction> {

    private final Money availableBalance;

    public SufficientFundsSpecification(Money availableBalance) {
        this.availableBalance = Objects.requireNonNull(availableBalance, "Le solde disponible est obligatoire");
    }

    /**
     * Retourne true si le solde disponible >= montant de la transaction.
     *
     * @throws Money.CurrencyMismatchException si les devises diffèrent
     */
    @Override
    public boolean isSatisfiedBy(Transaction transaction) {
        return availableBalance.isGreaterThanOrEqualTo(transaction.amount());
    }
}
