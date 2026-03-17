package com.uzima.domain.payment.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object : Montant monétaire avec devise.
 * Règles invariantes :
 * - mount >= 0 (jamais négatif)
 * - amount arrondi à la précision de la devise (2 décimales par défaut, 0 pour XOF/XAF)
 * - les opérations arithmétiques exigent la même devise
 * - subtract() interdit si le résultat serait négatif
 * Immuable (record Java). Toutes les opérations retournent une nouvelle instance.
 */
public record Money(BigDecimal amount, Currency currency) {

    // -------------------------------------------------------------------------
    // Constructeur canonique (validation)
    // -------------------------------------------------------------------------

    public Money {
        Objects.requireNonNull(amount, "Le montant ne peut pas être nul");
        Objects.requireNonNull(currency, "La devise ne peut pas être nulle");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountException(
                "Le montant ne peut pas être négatif : " + amount + " " + currency.symbol()
            );
        }
        // Normalisation : arrondi à l'échelle de la devise
        amount = amount.setScale(currency.defaultScale(), RoundingMode.HALF_EVEN);
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Factory principale.
     *
     * @param amount   Montant (doit être >= 0)
     * @param currency Devise
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Factory depuis une valeur long (évite les erreurs de parsing BigDecimal).
     */
    public static Money of(long amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Factory depuis des centimes (utile pour XOF/XAF dont l'unité est le franc entier).
     * Pour EUR/USD : 150 centimes = 1.50 EUR.
     * Pour XOF   : 1500 = 1500 XOF (pas de centime, mais la méthode reste cohérente).
     */
    public static Money ofCents(long cents, Currency currency) {
        int scale = currency.defaultScale();
        if (scale == 0) {
            return new Money(BigDecimal.valueOf(cents), currency);
        }
        BigDecimal divisor = BigDecimal.TEN.pow(scale);
        return new Money(BigDecimal.valueOf(cents).divide(divisor, scale, RoundingMode.HALF_EVEN), currency);
    }

    /**
     * Factory : zéro dans une devise donnée.
     */
    public static Money zero(Currency currency) {
        Objects.requireNonNull(currency, "La devise ne peut pas être nulle");
        return new Money(BigDecimal.ZERO, currency);
    }

    // -------------------------------------------------------------------------
    // Opérations arithmétiques
    // -------------------------------------------------------------------------

    /**
     * Addition. Les deux montants doivent être dans la même devise.
     *
     * @throws CurrencyMismatchException si les devises diffèrent
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Soustraction. Les deux montants doivent être dans la même devise.
     * Le résultat ne peut pas être négatif.
     *
     * @throws CurrencyMismatchException  si les devises diffèrent
     * @throws InsufficientFundsException si other > this
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        if (other.amount.compareTo(this.amount) > 0) {
            throw new InsufficientFundsException(
                "Fonds insuffisants : " + this + " < " + other
            );
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplication par un scalaire (ex: frais = amount * 0.015).
     *
     * @param multiplier Facteur multiplicatif (doit être > 0)
     */
    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Le multiplicateur ne peut pas être nul");
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountException("Le multiplicateur ne peut pas être négatif : " + multiplier);
        }
        return new Money(
            this.amount.multiply(multiplier).setScale(currency.defaultScale(), RoundingMode.HALF_EVEN),
            this.currency
        );
    }

    // -------------------------------------------------------------------------
    // Comparaisons
    // -------------------------------------------------------------------------

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // -------------------------------------------------------------------------
    // Affichage
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.displayName();
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "Le montant ne peut pas être nul");
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                "Impossible d'opérer sur des devises différentes : "
                + this.currency + " ≠ " + other.currency
            );
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions domaine imbriquées
    // -------------------------------------------------------------------------

    public static final class NegativeAmountException extends RuntimeException {
        public NegativeAmountException(String message) { super(message); }
    }

    public static final class CurrencyMismatchException extends RuntimeException {
        public CurrencyMismatchException(String message) { super(message); }
    }

    public static final class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) { super(message); }
    }
}
