package com.uzima.infrastructure.persistence.payment;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test d'intégration : TransactionRepositoryAdapter avec PostgreSQL réel (Testcontainers).
 * Utilise @DataJpaTest + Testcontainers pour tester la couche persistence sans mock.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("TransactionRepositoryAdapter — intégration PostgreSQL")
class TransactionRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("uzima_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Hibernate crée le schéma depuis les annotations @Entity — Flyway est dans bootstrap uniquement
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private SpringDataTransactionRepository jpaRepository;

    private TransactionRepositoryAdapter adapter;

    private final TimeProvider clock = () -> Instant.parse("2026-01-01T10:00:00Z");
    private final UserId sender    = UserId.generate();
    private final UserId recipient = UserId.generate();

    @BeforeEach
    void setUp() {
        adapter = new TransactionRepositoryAdapter(jpaRepository);
        jpaRepository.deleteAll();
    }

    private Transaction pendingTransaction() {
        return Transaction.initiate(
                sender, recipient,
                Money.of(5000L, Currency.XAF),
                PaymentMethod.MOBILE_MONEY,
                "Test paiement",
                clock
        );
    }

    // =========================================================================
    // @positive — save + findById
    // =========================================================================

    @Nested
    @DisplayName("@positive — save et findById")
    class SaveAndFind {

        @Test
        void save_andFindById_returnsEqualTransaction() {
            var tx = pendingTransaction();
            adapter.save(tx);

            var found = adapter.findById(tx.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(tx.id());
        }

        @Test
        void save_pendingTransaction_statusIsPending() {
            var tx = pendingTransaction();
            adapter.save(tx);

            var found = adapter.findById(tx.id()).orElseThrow();
            assertThat(found.isPending()).isTrue();
        }

        @Test
        void save_completedTransaction_statusIsCompleted() {
            var tx = pendingTransaction();
            tx.complete("EXT-001", clock);
            adapter.save(tx);

            var found = adapter.findById(tx.id()).orElseThrow();
            assertThat(found.isCompleted()).isTrue();
            assertThat(found.externalId()).contains("EXT-001");
        }

        @Test
        void save_preservesAllFields() {
            var tx = pendingTransaction();
            adapter.save(tx);

            var found = adapter.findById(tx.id()).orElseThrow();
            assertThat(found.senderId()).isEqualTo(sender);
            assertThat(found.recipientId()).isEqualTo(recipient);
            assertThat(found.amount()).isEqualTo(Money.of(5000L, Currency.XAF));
            assertThat(found.method()).isEqualTo(PaymentMethod.MOBILE_MONEY);
            assertThat(found.description()).contains("Test paiement");
        }
    }

    // =========================================================================
    // @edge-case — findById not found
    // =========================================================================

    @Nested
    @DisplayName("@edge-case — transaction introuvable")
    class NotFound {

        @Test
        void findById_withUnknownId_returnsEmpty() {
            var found = adapter.findById(TransactionId.generate());
            assertThat(found).isEmpty();
        }
    }

    // =========================================================================
    // @positive — findBySenderId / findByRecipientId
    // =========================================================================

    @Nested
    @DisplayName("@positive — recherche par expéditeur/destinataire")
    class FindByParty {

        @Test
        void findBySenderId_returnsOnlyTransactionsFromSender() {
            var tx1 = pendingTransaction();
            var tx2 = pendingTransaction();
            // transaction d'un autre expéditeur
            var otherSender = UserId.generate();
            var tx3 = Transaction.initiate(otherSender, recipient,
                    Money.of(100L, Currency.XAF), PaymentMethod.MOBILE_MONEY, null, clock);

            adapter.save(tx1);
            adapter.save(tx2);
            adapter.save(tx3);

            List<Transaction> sent = adapter.findBySenderId(sender, 10, 0);
            assertThat(sent).hasSize(2);
            assertThat(sent).allMatch(t -> t.senderId().equals(sender));
        }

        @Test
        void findByRecipientId_returnsOnlyTransactionsToRecipient() {
            var tx = pendingTransaction();
            adapter.save(tx);

            List<Transaction> received = adapter.findByRecipientId(recipient, 10, 0);
            assertThat(received).hasSize(1);
            assertThat(received.getFirst().recipientId()).isEqualTo(recipient);
        }
    }

    // =========================================================================
    // @positive — count
    // =========================================================================

    @Nested
    @DisplayName("@positive — comptage")
    class Count {

        @Test
        void countBySenderId_returnsCorrectCount() {
            adapter.save(pendingTransaction());
            adapter.save(pendingTransaction());

            assertThat(adapter.countBySenderId(sender)).isEqualTo(2);
        }

        @Test
        void countByRecipientId_returnsCorrectCount() {
            adapter.save(pendingTransaction());

            assertThat(adapter.countByRecipientId(recipient)).isEqualTo(1);
        }

        @Test
        void countBySenderId_whenNoTransactions_returnsZero() {
            assertThat(adapter.countBySenderId(UserId.generate())).isZero();
        }
    }

    // =========================================================================
    // @positive — update (save modifie l'état)
    // =========================================================================

    @Nested
    @DisplayName("@positive — mise à jour par re-save")
    class Update {

        @Test
        void save_twice_updatesTransaction() {
            var tx = pendingTransaction();
            adapter.save(tx);

            tx.fail("Raison d'échec", clock);
            adapter.save(tx);

            var found = adapter.findById(tx.id()).orElseThrow();
            assertThat(found.isFailed()).isTrue();
            assertThat(found.failureReason()).contains("Raison d'échec");
        }
    }

    // =========================================================================
    // @positive — pagination
    // =========================================================================

    @Nested
    @DisplayName("@positive — pagination")
    class Pagination {

        @Test
        void findBySenderId_withLimit_returnsAtMostLimitResults() {
            for (int i = 0; i < 5; i++) {
                adapter.save(pendingTransaction());
            }
            var page = adapter.findBySenderId(sender, 3, 0);
            assertThat(page).hasSize(3);
        }
    }
}
