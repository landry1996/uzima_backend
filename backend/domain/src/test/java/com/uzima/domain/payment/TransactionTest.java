package com.uzima.domain.payment;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.model.TransactionStatus;
import com.uzima.domain.payment.specification.SufficientFundsSpecification;
import com.uzima.domain.payment.specification.TransactionCompletedSpecification;
import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de l'Aggregate Root Transaction.
 *
 * Aucune dépendance Spring. TimeProvider fixe pour déterminisme.
 */
@DisplayName("Transaction (Aggregate Root)")
class TransactionTest {

    private static final Instant NOW  = Instant.parse("2026-03-12T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-12T10:05:00Z");

    private final TimeProvider clock      = () -> NOW;
    private final TimeProvider laterClock = () -> LATER;

    private UserId alice;
    private UserId bob;
    private Money amount;

    @BeforeEach
    void setUp() {
        alice  = UserId.generate();
        bob    = UserId.generate();
        amount = Money.of(new BigDecimal("5000"), Currency.XOF);
    }

    // -------------------------------------------------------------------------
    // initiate()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("initiate()")
    class InitiateTest {

        @Test
        @DisplayName("crée une transaction PENDING avec les bonnes données")
        void createsTransactionInPendingState() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, "Déjeuner", clock);

            assertThat(tx.id()).isNotNull();
            assertThat(tx.senderId()).isEqualTo(alice);
            assertThat(tx.recipientId()).isEqualTo(bob);
            assertThat(tx.amount()).isEqualTo(amount);
            assertThat(tx.method()).isEqualTo(PaymentMethod.MOBILE_MONEY);
            assertThat(tx.status()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.initiatedAt()).isEqualTo(NOW);
            assertThat(tx.description()).contains("Déjeuner");
        }

        @Test
        @DisplayName("description null est acceptée")
        void nullDescriptionIsAccepted() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.WALLET, null, clock);
            assertThat(tx.description()).isEmpty();
        }

        @Test
        @DisplayName("lève SelfPaymentException si sender == recipient")
        void throwsWhenSelfPayment() {
            assertThatThrownBy(() ->
                    Transaction.initiate(alice, alice, amount, PaymentMethod.MOBILE_MONEY, null, clock))
                    .isInstanceOf(Transaction.SelfPaymentException.class)
                    .hasMessageContaining("identiques");
        }

        @Test
        @DisplayName("lève NonPositiveAmountException si montant = 0")
        void throwsWhenZeroAmount() {
            Money zero = Money.zero(Currency.XOF);
            assertThatThrownBy(() ->
                    Transaction.initiate(alice, bob, zero, PaymentMethod.MOBILE_MONEY, null, clock))
                    .isInstanceOf(Transaction.NonPositiveAmountException.class);
        }

        @Test
        @DisplayName("lève InvalidDescriptionException si description > 255 chars")
        void throwsWhenDescriptionTooLong() {
            String longDesc = "A".repeat(256);
            assertThatThrownBy(() ->
                    Transaction.initiate(alice, bob, amount, PaymentMethod.CARD, longDesc, clock))
                    .isInstanceOf(Transaction.InvalidDescriptionException.class);
        }

        @Test
        @DisplayName("émet TransactionInitiatedEvent")
        void emitsTransactionInitiatedEvent() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            List<DomainEvent> events = tx.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Transaction.TransactionInitiatedEvent.class);

            Transaction.TransactionInitiatedEvent event = (Transaction.TransactionInitiatedEvent) events.get(0);
            assertThat(event.transactionId()).isEqualTo(tx.id());
            assertThat(event.senderId()).isEqualTo(alice);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("pullDomainEvents() vide la liste après appel")
        void pullDomainEventsClearsList() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.pullDomainEvents(); // premier appel

            assertThat(tx.pullDomainEvents()).isEmpty(); // second appel
        }
    }

    // -------------------------------------------------------------------------
    // complete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("complete()")
    class CompleteTest {

        @Test
        @DisplayName("passe la transaction en COMPLETED")
        void completesTransaction() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.pullDomainEvents(); // purge

            tx.complete("EXT-001", laterClock);

            assertThat(tx.status()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(tx.externalId()).contains("EXT-001");
            assertThat(tx.completedAt()).contains(LATER);
        }

        @Test
        @DisplayName("émet TransactionCompletedEvent")
        void emitsCompletedEvent() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.pullDomainEvents();
            tx.complete("EXT-001", laterClock);

            List<DomainEvent> events = tx.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Transaction.TransactionCompletedEvent.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si déjà COMPLETED")
        void throwsWhenAlreadyCompleted() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.complete("EXT-001", clock);

            assertThatThrownBy(() -> tx.complete("EXT-002", clock))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si FAILED")
        void throwsWhenFailed() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.fail("Erreur réseau", clock);

            assertThatThrownBy(() -> tx.complete("EXT-001", clock))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }
    }

    // -------------------------------------------------------------------------
    // fail()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("fail()")
    class FailTest {

        @Test
        @DisplayName("passe la transaction en FAILED avec la raison")
        void failsTransaction() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.CARD, null, clock);
            tx.pullDomainEvents();

            tx.fail("Carte refusée", laterClock);

            assertThat(tx.status()).isEqualTo(TransactionStatus.FAILED);
            assertThat(tx.failureReason()).contains("Carte refusée");
            assertThat(tx.failedAt()).contains(LATER);
        }

        @Test
        @DisplayName("émet TransactionFailedEvent")
        void emitsFailedEvent() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.CARD, null, clock);
            tx.pullDomainEvents();
            tx.fail("Erreur gateway", clock);

            List<DomainEvent> events = tx.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Transaction.TransactionFailedEvent.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si déjà COMPLETED")
        void throwsWhenAlreadyCompleted() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.complete("EXT-001", clock);

            assertThatThrownBy(() -> tx.fail("Erreur", clock))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }
    }

    // -------------------------------------------------------------------------
    // cancel()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("cancel()")
    class CancelTest {

        @Test
        @DisplayName("annule une transaction PENDING")
        void cancelsPendingTransaction() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.WALLET, null, clock);

            tx.cancel(laterClock);

            assertThat(tx.status()).isEqualTo(TransactionStatus.CANCELLED);
            assertThat(tx.cancelledAt()).contains(LATER);
            assertThat(tx.canBeCancelled()).isFalse();
        }

        @Test
        @DisplayName("lève IllegalTransitionException si COMPLETED")
        void throwsWhenCompleted() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.complete("EXT-001", clock);

            assertThatThrownBy(() -> tx.cancel(clock))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si FAILED")
        void throwsWhenFailed() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.fail("Erreur", clock);

            assertThatThrownBy(() -> tx.cancel(clock))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }
    }

    // -------------------------------------------------------------------------
    // reconstitute()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("reconstitute()")
    class ReconstitueTest {

        @Test
        @DisplayName("reconstitue une transaction COMPLETED depuis la DB")
        void reconstitutesCompletedTransaction() {
            TransactionId id = TransactionId.generate();

            Transaction tx = Transaction.reconstitute(
                id, alice, bob, amount, PaymentMethod.MOBILE_MONEY,
                "Test", TransactionStatus.COMPLETED, "EXT-999", null,
                NOW, LATER, null, null
            );

            assertThat(tx.id()).isEqualTo(id);
            assertThat(tx.status()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(tx.externalId()).contains("EXT-999");
            assertThat(tx.completedAt()).contains(LATER);
        }

        @Test
        @DisplayName("reconstitute() n'émet aucun domain event")
        void doesNotEmitDomainEvents() {
            Transaction tx = Transaction.reconstitute(
                TransactionId.generate(), alice, bob, amount, PaymentMethod.CARD,
                null, TransactionStatus.FAILED, null, "Erreur",
                NOW, null, LATER, null
            );

            assertThat(tx.pullDomainEvents()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Specifications
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Specifications")
    class SpecificationsTest {

        @Test
        @DisplayName("TransactionCompletedSpecification : true sur COMPLETED")
        void completedSpecMatchesCompleted() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.complete("EXT-001", clock);

            assertThat(new TransactionCompletedSpecification().isSatisfiedBy(tx)).isTrue();
        }

        @Test
        @DisplayName("TransactionCompletedSpecification : false sur PENDING")
        void completedSpecDoesNotMatchPending() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);

            assertThat(new TransactionCompletedSpecification().isSatisfiedBy(tx)).isFalse();
        }

        @Test
        @DisplayName("SufficientFundsSpecification : true si balance >= amount")
        void sufficientFundsMatchesWhenBalanceEnough() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.WALLET, null, clock);
            Money balance = Money.of(new BigDecimal("10000"), Currency.XOF);

            assertThat(new SufficientFundsSpecification(balance).isSatisfiedBy(tx)).isTrue();
        }

        @Test
        @DisplayName("SufficientFundsSpecification : false si balance < amount")
        void sufficientFundsDoesNotMatchWhenBalanceInsufficient() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.WALLET, null, clock);
            Money balance = Money.of(new BigDecimal("1000"), Currency.XOF);

            assertThat(new SufficientFundsSpecification(balance).isSatisfiedBy(tx)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Prédicats d'état
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Prédicats d'état")
    class PredicatesTest {

        @Test
        @DisplayName("isPending() est true uniquement sur PENDING")
        void isPendingOnlyWhenPending() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            assertThat(tx.isPending()).isTrue();
            assertThat(tx.isCompleted()).isFalse();
            assertThat(tx.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isCompleted() est true uniquement après complete()")
        void isCompletedOnlyAfterComplete() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.complete("EXT-100", clock);

            assertThat(tx.isCompleted()).isTrue();
            assertThat(tx.isPending()).isFalse();
            assertThat(tx.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isFailed() est true uniquement après fail()")
        void isFailedOnlyAfterFail() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.CARD, null, clock);
            tx.fail("Carte refusée", clock);

            assertThat(tx.isFailed()).isTrue();
            assertThat(tx.isPending()).isFalse();
            assertThat(tx.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("message d'erreur de transition mentionne [état terminal] pour un état terminal")
        void illegalTransitionMessageIncludesTerminalHint() {
            Transaction tx = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            tx.complete("EXT-001", clock);

            assertThatThrownBy(() -> tx.complete("EXT-002", clock))
                    .isInstanceOf(Transaction.IllegalTransitionException.class)
                    .hasMessageContaining("terminal");
        }
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Identité")
    class IdentityTest {

        @Test
        @DisplayName("deux transactions avec le même ID sont égales")
        void equalById() {
            TransactionId id = TransactionId.generate();
            Transaction tx1 = Transaction.reconstitute(
                id, alice, bob, amount, PaymentMethod.CARD,
                null, TransactionStatus.PENDING, null, null, NOW, null, null, null);
            Transaction tx2 = Transaction.reconstitute(
                id, alice, bob, amount, PaymentMethod.CARD,
                null, TransactionStatus.PENDING, null, null, NOW, null, null, null);

            assertThat(tx1).isEqualTo(tx2);
            assertThat(tx1.hashCode()).isEqualTo(tx2.hashCode());
        }

        @Test
        @DisplayName("deux transactions avec des IDs différents ne sont pas égales")
        void notEqualWithDifferentIds() {
            Transaction tx1 = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);
            Transaction tx2 = Transaction.initiate(alice, bob, amount, PaymentMethod.MOBILE_MONEY, null, clock);

            assertThat(tx1).isNotEqualTo(tx2);
        }
    }
}
