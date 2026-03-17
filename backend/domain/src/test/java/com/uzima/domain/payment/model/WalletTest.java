package com.uzima.domain.payment.model;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de l'Aggregate Root Wallet.
 *
 * Aucune dépendance Spring. TimeProvider fixe pour déterminisme.
 */
@DisplayName("Wallet — Aggregate Root")
class WalletTest {

    private static final Instant NOW   = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T10:05:00Z");

    private final TimeProvider clock      = () -> NOW;
    private final TimeProvider laterClock = () -> LATER;

    private UserId ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UserId.generate();
    }

    // =========================================================================
    // @positive — création
    // =========================================================================

    @Nested
    @DisplayName("@positive — création")
    class Creation {

        @Test
        @DisplayName("create() démarre avec un solde zéro")
        void create_startsWithZeroBalance() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);

            assertThat(wallet.balance().isZero()).isTrue();
            assertThat(wallet.balance().currency()).isEqualTo(Currency.XAF);
        }

        @Test
        @DisplayName("create() génère un identifiant non nul")
        void create_generatesNonNullId() {
            var wallet = Wallet.create(ownerId, Currency.XOF, clock);

            assertThat(wallet.id()).isNotNull();
        }

        @Test
        @DisplayName("create() stocke le propriétaire")
        void create_storesOwnerId() {
            var wallet = Wallet.create(ownerId, Currency.EUR, clock);

            assertThat(wallet.ownerId()).isEqualTo(ownerId);
        }

        @Test
        @DisplayName("create() n'émet aucun événement de domaine")
        void create_emitsNoDomainEvents() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);

            assertThat(wallet.pullDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("create() fixe createdAt à l'heure du clock")
        void create_setsCreatedAt() {
            var wallet = Wallet.create(ownerId, Currency.XOF, clock);

            assertThat(wallet.createdAt()).isEqualTo(NOW);
        }
    }

    // =========================================================================
    // @positive — credit()
    // =========================================================================

    @Nested
    @DisplayName("@positive — credit()")
    class CreditOperation {

        @Test
        @DisplayName("credit() augmente le solde")
        void credit_increasesBalance() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            var amount = Money.of(5000L, Currency.XAF);

            wallet.credit(amount, laterClock);

            assertThat(wallet.balance().amount()).isEqualByComparingTo("5000");
        }

        @Test
        @DisplayName("credit() émet un WalletCreditedEvent")
        void credit_emitsWalletCreditedEvent() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            wallet.pullDomainEvents(); // purge creation events (none, but be safe)

            wallet.credit(Money.of(1000L, Currency.XAF), laterClock);

            var events = wallet.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(Wallet.WalletCreditedEvent.class);
        }

        @Test
        @DisplayName("credit() consécutifs accumulent le solde correctement")
        void credit_consecutiveCreditAccumulatesBalance() {
            var wallet = Wallet.create(ownerId, Currency.XOF, clock);

            wallet.credit(Money.of(1000L, Currency.XOF), clock);
            wallet.credit(Money.of(2000L, Currency.XOF), clock);

            assertThat(wallet.balance().amount()).isEqualByComparingTo("3000");
        }

        @Test
        @DisplayName("WalletCreditedEvent contient le montant crédité et le solde après")
        void credit_eventContainsCorrectFields() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            var amount = Money.of(3000L, Currency.XAF);

            wallet.credit(amount, laterClock);

            var event = (Wallet.WalletCreditedEvent) wallet.pullDomainEvents().getFirst();
            assertThat(event.amount()).isEqualTo(amount);
            assertThat(event.balanceAfter().amount()).isEqualByComparingTo("3000");
            assertThat(event.occurredAt()).isEqualTo(LATER);
            assertThat(event.ownerId()).isEqualTo(ownerId);
        }
    }

    // =========================================================================
    // @positive — debit()
    // =========================================================================

    @Nested
    @DisplayName("@positive — debit()")
    class DebitOperation {

        @Test
        @DisplayName("debit() diminue le solde")
        void debit_decreasesBalance() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            wallet.credit(Money.of(10000L, Currency.XAF), clock);
            wallet.pullDomainEvents();

            wallet.debit(Money.of(4000L, Currency.XAF), laterClock);

            assertThat(wallet.balance().amount()).isEqualByComparingTo("6000");
        }

        @Test
        @DisplayName("debit() émet un WalletDebitedEvent")
        void debit_emitsWalletDebitedEvent() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            wallet.credit(Money.of(5000L, Currency.XAF), clock);
            wallet.pullDomainEvents();

            wallet.debit(Money.of(2000L, Currency.XAF), laterClock);

            var events = wallet.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(Wallet.WalletDebitedEvent.class);
        }

        @Test
        @DisplayName("debit() exact vide le solde (résultat zéro)")
        void debit_exactAmountResultsInZeroBalance() {
            var wallet = Wallet.create(ownerId, Currency.XOF, clock);
            wallet.credit(Money.of(500L, Currency.XOF), clock);
            wallet.pullDomainEvents();

            wallet.debit(Money.of(500L, Currency.XOF), clock);

            assertThat(wallet.balance().isZero()).isTrue();
        }

        @Test
        @DisplayName("WalletDebitedEvent contient le bon solde après débit")
        void debit_eventContainsCorrectBalanceAfter() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            wallet.credit(Money.of(8000L, Currency.XAF), clock);
            wallet.pullDomainEvents();

            wallet.debit(Money.of(3000L, Currency.XAF), laterClock);

            var event = (Wallet.WalletDebitedEvent) wallet.pullDomainEvents().getFirst();
            assertThat(event.balanceAfter().amount()).isEqualByComparingTo("5000");
            assertThat(event.occurredAt()).isEqualTo(LATER);
            assertThat(event.ownerId()).isEqualTo(ownerId);
        }
    }

    // =========================================================================
    // @negative — fonds insuffisants
    // =========================================================================

    @Nested
    @DisplayName("@negative — fonds insuffisants")
    class InsufficientFunds {

        @Test
        @DisplayName("debit() lance InsufficientFundsException si solde insuffisant")
        void debit_throwsInsufficientFundsExceptionWhenBalanceLow() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            // solde = 0

            assertThatThrownBy(() -> wallet.debit(Money.of(100L, Currency.XAF), clock))
                    .isInstanceOf(Money.InsufficientFundsException.class);
        }

        @Test
        @DisplayName("debit() ne modifie pas le solde en cas d'exception")
        void debit_doesNotModifyBalanceOnException() {
            var wallet = Wallet.create(ownerId, Currency.XOF, clock);
            wallet.credit(Money.of(200L, Currency.XOF), clock);
            wallet.pullDomainEvents();

            assertThatThrownBy(() -> wallet.debit(Money.of(500L, Currency.XOF), clock))
                    .isInstanceOf(Money.InsufficientFundsException.class);

            assertThat(wallet.balance().amount()).isEqualByComparingTo("200");
        }
    }

    // =========================================================================
    // @negative — devise mismatch
    // =========================================================================

    @Nested
    @DisplayName("@negative — devise mismatch")
    class CurrencyMismatch {

        @Test
        @DisplayName("credit() avec une devise différente lance CurrencyMismatchException")
        void credit_differentCurrency_throwsCurrencyMismatch() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);

            assertThatThrownBy(() -> wallet.credit(Money.of(100L, Currency.EUR), clock))
                    .isInstanceOf(Money.CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("debit() avec une devise différente lance CurrencyMismatchException")
        void debit_differentCurrency_throwsCurrencyMismatch() {
            var wallet = Wallet.create(ownerId, Currency.XOF, clock);
            wallet.credit(Money.of(1000L, Currency.XOF), clock);
            wallet.pullDomainEvents();

            assertThatThrownBy(() -> wallet.debit(Money.of(100L, Currency.USD), clock))
                    .isInstanceOf(Money.CurrencyMismatchException.class);
        }
    }

    // =========================================================================
    // @positive — pullDomainEvents() drain
    // =========================================================================

    @Nested
    @DisplayName("@positive — pullDomainEvents()")
    class DomainEvents {

        @Test
        @DisplayName("pullDomainEvents() draine les événements (second appel retourne liste vide)")
        void pullDomainEvents_drainsEvents() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            wallet.credit(Money.of(500L, Currency.XAF), clock);

            wallet.pullDomainEvents(); // premier appel — draine

            assertThat(wallet.pullDomainEvents()).isEmpty(); // second appel — vide
        }

        @Test
        @DisplayName("pullDomainEvents() retourne les événements de credit et debit dans l'ordre")
        void pullDomainEvents_returnsEventsInOrder() {
            var wallet = Wallet.create(ownerId, Currency.XAF, clock);
            wallet.credit(Money.of(10000L, Currency.XAF), clock);
            wallet.debit(Money.of(3000L, Currency.XAF), clock);

            var events = wallet.pullDomainEvents();

            assertThat(events).hasSize(2);
            assertThat(events.getFirst()).isInstanceOf(Wallet.WalletCreditedEvent.class);
            assertThat(events.getLast()).isInstanceOf(Wallet.WalletDebitedEvent.class);
        }
    }

    // =========================================================================
    // @positive — reconstitute()
    // =========================================================================

    @Nested
    @DisplayName("@positive — reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("reconstitute() recrée le wallet avec le bon solde depuis la persistance")
        void reconstitute_preservesAllFields() {
            var walletId = WalletId.generate();
            var balance  = Money.of(7500L, Currency.XOF);

            var wallet = Wallet.reconstitute(walletId, ownerId, balance, NOW, LATER);

            assertThat(wallet.id()).isEqualTo(walletId);
            assertThat(wallet.ownerId()).isEqualTo(ownerId);
            assertThat(wallet.balance()).isEqualTo(balance);
            assertThat(wallet.createdAt()).isEqualTo(NOW);
            assertThat(wallet.updatedAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("reconstitute() n'émet aucun événement de domaine")
        void reconstitute_emitsNoDomainEvents() {
            var wallet = Wallet.reconstitute(
                    WalletId.generate(), ownerId,
                    Money.of(1000L, Currency.XAF),
                    NOW, NOW
            );

            assertThat(wallet.pullDomainEvents()).isEmpty();
        }
    }
}
