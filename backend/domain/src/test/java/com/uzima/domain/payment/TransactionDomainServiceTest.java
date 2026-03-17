package com.uzima.domain.payment;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.model.TransactionStatus;
import com.uzima.domain.payment.port.TransactionRepository;
import com.uzima.domain.payment.service.TransactionDomainService;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du service de domaine TransactionDomainService.
 *
 * Utilise InMemoryTransactionRepository — aucune dépendance Spring/JPA/Mockito.
 */
@DisplayName("TransactionDomainService")
class TransactionDomainServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> NOW;

    private InMemoryTransactionRepository repository;
    private TransactionDomainService service;

    private UserId alice;
    private UserId bob;
    private UserId charlie;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        service    = new TransactionDomainService(repository);
        alice      = UserId.generate();
        bob        = UserId.generate();
        charlie    = UserId.generate();
    }

    // -------------------------------------------------------------------------
    // getSentTransactions()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getSentTransactions()")
    class GetSentTransactionsTest {

        @Test
        @DisplayName("retourne les transactions envoyées par l'expéditeur")
        void returnsSentTransactions() {
            Transaction t1 = Transaction.initiate(alice, bob,     money(1000), PaymentMethod.MOBILE_MONEY, null, clock);
            Transaction t2 = Transaction.initiate(alice, charlie, money(2000), PaymentMethod.WALLET,       null, clock);
            Transaction t3 = Transaction.initiate(bob,   charlie, money(3000), PaymentMethod.MOBILE_MONEY, null, clock); // pas alice
            repository.save(t1);
            repository.save(t2);
            repository.save(t3);

            List<Transaction> result = service.getSentTransactions(alice, 10, 0);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(tx -> tx.senderId().equals(alice));
        }

        @Test
        @DisplayName("respecte la pagination (limit / offset)")
        void respectsPagination() {
            for (int i = 0; i < 5; i++) {
                repository.save(Transaction.initiate(alice, bob, money(100 * (i + 1)), PaymentMethod.WALLET, null, clock));
            }

            assertThat(service.getSentTransactions(alice, 2, 0)).hasSize(2);
            assertThat(service.getSentTransactions(alice, 2, 4)).hasSize(1);
        }

        @Test
        @DisplayName("retourne une liste vide si aucune transaction")
        void returnsEmptyListWhenNoTransactions() {
            assertThat(service.getSentTransactions(alice, 10, 0)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getReceivedTransactions()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getReceivedTransactions()")
    class GetReceivedTransactionsTest {

        @Test
        @DisplayName("retourne les transactions reçues par le destinataire")
        void returnsReceivedTransactions() {
            Transaction t1 = Transaction.initiate(alice,   bob, money(500),  PaymentMethod.MOBILE_MONEY, null, clock);
            Transaction t2 = Transaction.initiate(charlie, bob, money(1500), PaymentMethod.CARD,         null, clock);
            Transaction t3 = Transaction.initiate(alice,   charlie, money(2000), PaymentMethod.WALLET,   null, clock); // pas bob
            repository.save(t1);
            repository.save(t2);
            repository.save(t3);

            List<Transaction> result = service.getReceivedTransactions(bob, 10, 0);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(tx -> tx.recipientId().equals(bob));
        }

        @Test
        @DisplayName("retourne une liste vide si aucune transaction reçue")
        void returnsEmptyListWhenNoneReceived() {
            repository.save(Transaction.initiate(alice, bob, money(1000), PaymentMethod.WALLET, null, clock));

            assertThat(service.getReceivedTransactions(alice, 10, 0)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // countSentTransactions()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("countSentTransactions()")
    class CountSentTest {

        @Test
        @DisplayName("compte correctement les transactions envoyées")
        void countsCorrectly() {
            repository.save(Transaction.initiate(alice, bob,     money(100), PaymentMethod.WALLET, null, clock));
            repository.save(Transaction.initiate(alice, charlie, money(200), PaymentMethod.WALLET, null, clock));
            repository.save(Transaction.initiate(bob,   alice,   money(300), PaymentMethod.WALLET, null, clock)); // pas alice→x

            assertThat(service.countSentTransactions(alice)).isEqualTo(2L);
        }

        @Test
        @DisplayName("retourne 0 si aucune transaction envoyée")
        void returnsZeroWhenNone() {
            assertThat(service.countSentTransactions(alice)).isEqualTo(0L);
        }
    }

    // -------------------------------------------------------------------------
    // countReceivedTransactions()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("countReceivedTransactions()")
    class CountReceivedTest {

        @Test
        @DisplayName("compte correctement les transactions reçues")
        void countsCorrectly() {
            repository.save(Transaction.initiate(alice,   bob, money(100), PaymentMethod.MOBILE_MONEY, null, clock));
            repository.save(Transaction.initiate(charlie, bob, money(200), PaymentMethod.MOBILE_MONEY, null, clock));
            repository.save(Transaction.initiate(alice, charlie, money(300), PaymentMethod.WALLET,     null, clock)); // pas →bob

            assertThat(service.countReceivedTransactions(bob)).isEqualTo(2L);
        }

        @Test
        @DisplayName("retourne 0 si aucune transaction reçue")
        void returnsZeroWhenNone() {
            assertThat(service.countReceivedTransactions(charlie)).isEqualTo(0L);
        }
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getById()")
    class GetByIdTest {

        @Test
        @DisplayName("retourne la transaction si elle existe")
        void returnsTransactionWhenFound() {
            Transaction tx = Transaction.initiate(alice, bob, money(1000), PaymentMethod.WALLET, null, clock);
            repository.save(tx);

            Transaction found = service.getById(tx.id());
            assertThat(found).isEqualTo(tx);
        }

        @Test
        @DisplayName("lève TransactionNotFoundException si introuvable")
        void throwsWhenNotFound() {
            TransactionId unknownId = TransactionId.generate();

            assertThatThrownBy(() -> service.getById(unknownId))
                    .isInstanceOf(TransactionDomainService.TransactionNotFoundException.class)
                    .hasMessageContaining(unknownId.value().toString());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Money money(long amount) {
        return Money.of(amount, Currency.XOF);
    }

    // -------------------------------------------------------------------------
    // InMemoryTransactionRepository (stub sans Mockito)
    // -------------------------------------------------------------------------

    static final class InMemoryTransactionRepository implements TransactionRepository {

        private final List<Transaction> store = new ArrayList<>();

        @Override
        public void save(Transaction transaction) {
            store.removeIf(tx -> tx.id().equals(transaction.id()));
            store.add(transaction);
        }

        @Override
        public Optional<Transaction> findById(TransactionId id) {
            return store.stream().filter(tx -> tx.id().equals(id)).findFirst();
        }

        @Override
        public List<Transaction> findBySenderId(UserId senderId, int limit, int offset) {
            return store.stream()
                    .filter(tx -> tx.senderId().equals(senderId))
                    .sorted(Comparator.comparing(Transaction::initiatedAt).reversed())
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<Transaction> findByRecipientId(UserId recipientId, int limit, int offset) {
            return store.stream()
                    .filter(tx -> tx.recipientId().equals(recipientId))
                    .sorted(Comparator.comparing(Transaction::initiatedAt).reversed())
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countBySenderId(UserId senderId) {
            return store.stream().filter(tx -> tx.senderId().equals(senderId)).count();
        }

        @Override
        public long countByRecipientId(UserId recipientId) {
            return store.stream().filter(tx -> tx.recipientId().equals(recipientId)).count();
        }
    }
}
