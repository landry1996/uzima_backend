package com.uzima.domain.payment;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du Value Object Money.
 *
 * Aucune dépendance Spring. Pur DDD.
 */
@DisplayName("Money (Value Object)")
class MoneyTest {

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Factories")
    class FactoriesTest {

        @Test
        @DisplayName("of(BigDecimal, Currency) crée un montant valide")
        void ofBigDecimalCreatesValidMoney() {
            Money money = Money.of(new BigDecimal("100.50"), Currency.EUR);

            assertThat(money.amount()).isEqualByComparingTo("100.50");
            assertThat(money.currency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("of(long, Currency) crée un montant valide")
        void ofLongCreatesValidMoney() {
            Money money = Money.of(1500L, Currency.XOF);

            assertThat(money.amount()).isEqualByComparingTo("1500");
            assertThat(money.currency()).isEqualTo(Currency.XOF);
        }

        @Test
        @DisplayName("zero(Currency) crée un montant nul")
        void zeroCreatesZeroMoney() {
            Money money = Money.zero(Currency.EUR);

            assertThat(money.isZero()).isTrue();
            assertThat(money.currency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("ofCents(150, EUR) = 1.50 EUR")
        void ofCentsMapsCorrectlyForEur() {
            Money money = Money.ofCents(150L, Currency.EUR);

            assertThat(money.amount()).isEqualByComparingTo("1.50");
        }

        @Test
        @DisplayName("ofCents pour XOF retourne le montant entier (pas de centime)")
        void ofCentsMapsCorrectlyForXof() {
            Money money = Money.ofCents(1500L, Currency.XOF);

            assertThat(money.amount()).isEqualByComparingTo("1500");
        }

        @Test
        @DisplayName("lève NegativeAmountException si montant < 0")
        void throwsWhenNegativeAmount() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01"), Currency.EUR))
                    .isInstanceOf(Money.NegativeAmountException.class);
        }

        @Test
        @DisplayName("lève NullPointerException si devise nulle")
        void throwsWhenCurrencyNull() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("montant zéro exact est accepté")
        void zeroAmountIsAccepted() {
            Money money = Money.of(BigDecimal.ZERO, Currency.USD);
            assertThat(money.isZero()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Normalisation de l'échelle
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Normalisation échelle devise")
    class ScaleNormalizationTest {

        @Test
        @DisplayName("EUR : arrondi à 2 décimales (HALF_EVEN)")
        void eurRoundsToTwoDecimals() {
            Money money = Money.of(new BigDecimal("10.005"), Currency.EUR);
            assertThat(money.amount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("XOF : arrondi à 0 décimale")
        void xofRoundsToZeroDecimals() {
            Money money = Money.of(new BigDecimal("1500.0"), Currency.XOF);
            assertThat(money.amount().scale()).isEqualTo(0);
        }
    }

    // -------------------------------------------------------------------------
    // Addition
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("add()")
    class AddTest {

        @Test
        @DisplayName("additionne deux montants de même devise")
        void addsSameCurrency() {
            Money a = Money.of(new BigDecimal("100.00"), Currency.EUR);
            Money b = Money.of(new BigDecimal("50.50"), Currency.EUR);

            Money result = a.add(b);

            assertThat(result.amount()).isEqualByComparingTo("150.50");
            assertThat(result.currency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("lève CurrencyMismatchException si devises différentes")
        void throwsWhenDifferentCurrencies() {
            Money eur = Money.of(BigDecimal.TEN, Currency.EUR);
            Money usd = Money.of(BigDecimal.TEN, Currency.USD);

            assertThatThrownBy(() -> eur.add(usd))
                    .isInstanceOf(Money.CurrencyMismatchException.class)
                    .hasMessageContaining("EUR")
                    .hasMessageContaining("USD");
        }

        @Test
        @DisplayName("addition avec zéro retourne le montant initial")
        void addZeroReturnsSameAmount() {
            Money money = Money.of(new BigDecimal("200.00"), Currency.EUR);
            Money result = money.add(Money.zero(Currency.EUR));

            assertThat(result.amount()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("add() ne modifie pas les instances originales (immutabilité)")
        void addIsImmutable() {
            Money a = Money.of(new BigDecimal("100.00"), Currency.EUR);
            Money b = Money.of(new BigDecimal("50.00"), Currency.EUR);
            a.add(b);

            assertThat(a.amount()).isEqualByComparingTo("100.00");
            assertThat(b.amount()).isEqualByComparingTo("50.00");
        }
    }

    // -------------------------------------------------------------------------
    // Soustraction
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("subtract()")
    class SubtractTest {

        @Test
        @DisplayName("soustrait si solde suffisant")
        void subtractsWhenSufficient() {
            Money a = Money.of(new BigDecimal("100.00"), Currency.EUR);
            Money b = Money.of(new BigDecimal("30.00"), Currency.EUR);

            Money result = a.subtract(b);

            assertThat(result.amount()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("soustraction exacte retourne zéro")
        void subtractExactAmountReturnsZero() {
            Money a = Money.of(new BigDecimal("50.00"), Currency.EUR);
            Money result = a.subtract(a);

            assertThat(result.isZero()).isTrue();
        }

        @Test
        @DisplayName("lève InsufficientFundsException si montant > solde")
        void throwsWhenInsufficientFunds() {
            Money balance = Money.of(new BigDecimal("20.00"), Currency.EUR);
            Money amount  = Money.of(new BigDecimal("50.00"), Currency.EUR);

            assertThatThrownBy(() -> balance.subtract(amount))
                    .isInstanceOf(Money.InsufficientFundsException.class);
        }

        @Test
        @DisplayName("lève CurrencyMismatchException si devises différentes")
        void throwsWhenDifferentCurrencies() {
            Money eur = Money.of(BigDecimal.TEN, Currency.EUR);
            Money xof = Money.of(BigDecimal.TEN, Currency.XOF);

            assertThatThrownBy(() -> eur.subtract(xof))
                    .isInstanceOf(Money.CurrencyMismatchException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Multiplication
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("multiply()")
    class MultiplyTest {

        @Test
        @DisplayName("multiplie par un scalaire positif")
        void multipliesCorrectly() {
            Money money = Money.of(new BigDecimal("100.00"), Currency.EUR);

            Money result = money.multiply(new BigDecimal("0.015")); // frais 1.5%

            assertThat(result.amount()).isEqualByComparingTo("1.50");
            assertThat(result.currency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("multiplication par zéro retourne zéro")
        void multiplyByZeroReturnsZero() {
            Money money = Money.of(new BigDecimal("500.00"), Currency.EUR);
            assertThat(money.multiply(BigDecimal.ZERO).isZero()).isTrue();
        }

        @Test
        @DisplayName("lève NegativeAmountException si multiplicateur < 0")
        void throwsWhenNegativeMultiplier() {
            Money money = Money.of(BigDecimal.TEN, Currency.EUR);

            assertThatThrownBy(() -> money.multiply(new BigDecimal("-1")))
                    .isInstanceOf(Money.NegativeAmountException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Comparaisons
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Comparaisons")
    class ComparisonTest {

        @Test
        @DisplayName("isGreaterThan retourne true si this > other")
        void isGreaterThan() {
            Money big   = Money.of(BigDecimal.TEN, Currency.EUR);
            Money small = Money.of(BigDecimal.ONE, Currency.EUR);
            assertThat(big.isGreaterThan(small)).isTrue();
            assertThat(small.isGreaterThan(big)).isFalse();
        }

        @Test
        @DisplayName("isGreaterThanOrEqualTo retourne true si this == other")
        void isGreaterThanOrEqualTo() {
            Money a = Money.of(BigDecimal.TEN, Currency.EUR);
            Money b = Money.of(BigDecimal.TEN, Currency.EUR);
            assertThat(a.isGreaterThanOrEqualTo(b)).isTrue();
        }

        @Test
        @DisplayName("isLessThan retourne true si this < other")
        void isLessThan() {
            Money small = Money.of(BigDecimal.ONE, Currency.EUR);
            Money big   = Money.of(BigDecimal.TEN, Currency.EUR);
            assertThat(small.isLessThan(big)).isTrue();
        }

        @Test
        @DisplayName("isPositive retourne false pour un montant nul")
        void isPositiveReturnsFalseForZero() {
            assertThat(Money.zero(Currency.EUR).isPositive()).isFalse();
        }

        @Test
        @DisplayName("lève CurrencyMismatchException sur comparaison cross-devise")
        void throwsOnCrossCurrencyComparison() {
            Money eur = Money.of(BigDecimal.TEN, Currency.EUR);
            Money usd = Money.of(BigDecimal.TEN, Currency.USD);

            assertThatThrownBy(() -> eur.isGreaterThan(usd))
                    .isInstanceOf(Money.CurrencyMismatchException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Égalité record
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Égalité (record)")
    class EqualityTest {

        @Test
        @DisplayName("deux Money identiques (même montant, même devise) sont égaux")
        void equalWhenSameAmountAndCurrency() {
            Money a = Money.of(new BigDecimal("100.00"), Currency.EUR);
            Money b = Money.of(new BigDecimal("100.00"), Currency.EUR);
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("deux Money avec montants différents ne sont pas égaux")
        void notEqualWhenDifferentAmounts() {
            Money a = Money.of(new BigDecimal("100.00"), Currency.EUR);
            Money b = Money.of(new BigDecimal("200.00"), Currency.EUR);
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("deux Money avec devises différentes ne sont pas égaux")
        void notEqualWhenDifferentCurrencies() {
            Money eur = Money.of(BigDecimal.TEN, Currency.EUR);
            Money usd = Money.of(BigDecimal.TEN, Currency.USD);
            assertThat(eur).isNotEqualTo(usd);
        }
    }
}
