package com.uzima.infrastructure.persistence.payment;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.model.TransactionStatus;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du mapper TransactionEntityMapper.
 *
 * Aucune dépendance Spring. Test pur de conversion domaine ↔ JPA.
 */
@DisplayName("TransactionEntityMapper")
class TransactionEntityMapperTest {

    private static final Instant NOW   = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T10:05:00Z");

    private final TimeProvider clock = () -> NOW;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Transaction initiatedTransaction() {
        var sender    = UserId.generate();
        var recipient = UserId.generate();
        return Transaction.initiate(
                sender, recipient,
                Money.of(5000L, Currency.XAF),
                PaymentMethod.MOBILE_MONEY,
                "Test mapper",
                clock
        );
    }

    private Transaction completedTransaction() {
        var sender    = UserId.generate();
        var recipient = UserId.generate();
        var tx = Transaction.initiate(
                sender, recipient,
                Money.of(2500L, Currency.XOF),
                PaymentMethod.WALLET,
                "Test completed",
                clock
        );
        tx.pullDomainEvents();
        tx.complete("EXT-MAPPER-001", clock);
        tx.pullDomainEvents();
        return tx;
    }

    private Transaction reconstitutedTransaction(UUID id, UUID senderId, UUID recipientId) {
        return Transaction.reconstitute(
                TransactionId.of(id),
                UserId.of(senderId),
                UserId.of(recipientId),
                Money.of(new BigDecimal("1000"), Currency.EUR),
                PaymentMethod.CARD,
                "Description reconstituée",
                TransactionStatus.PENDING,
                null, null,
                NOW, null, null, null
        );
    }

    // =========================================================================
    // @positive — toJpaEntity()
    // =========================================================================

    @Nested
    @DisplayName("@positive — toJpaEntity()")
    class ToJpaEntity {

        @Test
        @DisplayName("toJpaEntity() mappe l'identifiant correctement")
        void toJpaEntity_mapsIdCorrectly() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getId()).isEqualTo(tx.id().value());
        }

        @Test
        @DisplayName("toJpaEntity() mappe senderId et recipientId correctement")
        void toJpaEntity_mapsSenderAndRecipientIds() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getSenderId()).isEqualTo(tx.senderId().value());
            assertThat(entity.getRecipientId()).isEqualTo(tx.recipientId().value());
        }

        @Test
        @DisplayName("toJpaEntity() mappe le montant correctement")
        void toJpaEntity_mapsAmountCorrectly() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getAmount()).isEqualByComparingTo(tx.amount().amount());
        }

        @Test
        @DisplayName("toJpaEntity() mappe la devise comme nom d'enum")
        void toJpaEntity_mapsCurrencyAsEnumName() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getCurrency()).isEqualTo(Currency.XAF.name());
        }

        @Test
        @DisplayName("toJpaEntity() mappe la méthode de paiement")
        void toJpaEntity_mapsPaymentMethod() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getMethod()).isEqualTo(PaymentMethod.MOBILE_MONEY);
        }

        @Test
        @DisplayName("toJpaEntity() mappe le statut PENDING")
        void toJpaEntity_mapsPendingStatus() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getStatus()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test
        @DisplayName("toJpaEntity() mappe la description non nulle")
        void toJpaEntity_mapsDescription() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getDescription()).isEqualTo("Test mapper");
        }

        @Test
        @DisplayName("toJpaEntity() mappe initiatedAt correctement")
        void toJpaEntity_mapsInitiatedAt() {
            var tx     = initiatedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getInitiatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toJpaEntity() mappe completedAt pour une transaction COMPLETED")
        void toJpaEntity_mapsCompletedAt() {
            var tx     = completedTransaction();
            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(entity.getCompletedAt()).isNotNull();
            assertThat(entity.getExternalId()).isEqualTo("EXT-MAPPER-001");
        }
    }

    // =========================================================================
    // @positive — toDomain()
    // =========================================================================

    @Nested
    @DisplayName("@positive — toDomain()")
    class ToDomain {

        @Test
        @DisplayName("toDomain() reconstruit un domaine Transaction depuis l'entité JPA")
        void toDomain_reconstructsDomainTransaction() {
            var senderId    = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var txId        = UUID.randomUUID();

            var entity = TransactionJpaEntity.of(
                    txId,
                    senderId,
                    recipientId,
                    new BigDecimal("3000"),
                    "XOF",
                    PaymentMethod.MOBILE_MONEY,
                    TransactionStatus.PENDING,
                    "Paiement test",
                    null, null,
                    NOW, null, null, null
            );

            var domain = TransactionEntityMapper.toDomain(entity);

            assertThat(domain.id().value()).isEqualTo(txId);
            assertThat(domain.senderId().value()).isEqualTo(senderId);
            assertThat(domain.recipientId().value()).isEqualTo(recipientId);
            assertThat(domain.amount().amount()).isEqualByComparingTo("3000");
            assertThat(domain.amount().currency()).isEqualTo(Currency.XOF);
            assertThat(domain.method()).isEqualTo(PaymentMethod.MOBILE_MONEY);
            assertThat(domain.status()).isEqualTo(TransactionStatus.PENDING);
            assertThat(domain.initiatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("toDomain() reconstruit une transaction COMPLETED avec externalId")
        void toDomain_reconstructsCompletedTransaction() {
            var entity = TransactionJpaEntity.of(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("10000"),
                    "XAF",
                    PaymentMethod.CARD,
                    TransactionStatus.COMPLETED,
                    null,
                    "EXT-9999",
                    null,
                    NOW, LATER, null, null
            );

            var domain = TransactionEntityMapper.toDomain(entity);

            assertThat(domain.status()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(domain.externalId()).contains("EXT-9999");
            assertThat(domain.completedAt()).contains(LATER);
        }

        @Test
        @DisplayName("toDomain() reconstruit une transaction FAILED avec failureReason")
        void toDomain_reconstructsFailedTransaction() {
            var entity = TransactionJpaEntity.of(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("500"),
                    "EUR",
                    PaymentMethod.CARD,
                    TransactionStatus.FAILED,
                    null,
                    null,
                    "Carte refusée",
                    NOW, null, LATER, null
            );

            var domain = TransactionEntityMapper.toDomain(entity);

            assertThat(domain.status()).isEqualTo(TransactionStatus.FAILED);
            assertThat(domain.failureReason()).contains("Carte refusée");
            assertThat(domain.failedAt()).contains(LATER);
        }
    }

    // =========================================================================
    // @edge-case — null description et null externalId
    // =========================================================================

    @Nested
    @DisplayName("@edge-case — valeurs nulles optionnelles")
    class NullOptionals {

        @Test
        @DisplayName("toJpaEntity() préserve une description nulle (Optional.empty)")
        void toJpaEntity_preservesNullDescription() {
            var sender    = UserId.generate();
            var recipient = UserId.generate();
            var tx = Transaction.initiate(
                    sender, recipient,
                    Money.of(1000L, Currency.XOF),
                    PaymentMethod.MOBILE_MONEY,
                    null, // description nulle
                    clock
            );

            var entity = TransactionEntityMapper.toJpaEntity(tx);

            assertThat(entity.getDescription()).isNull();
        }

        @Test
        @DisplayName("toDomain() préserve une description nulle → Optional.empty()")
        void toDomain_preservesNullDescription() {
            var entity = TransactionJpaEntity.of(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("1000"),
                    "XOF",
                    PaymentMethod.MOBILE_MONEY,
                    TransactionStatus.PENDING,
                    null,  // description nulle
                    null, null,
                    NOW, null, null, null
            );

            var domain = TransactionEntityMapper.toDomain(entity);

            assertThat(domain.description()).isEmpty();
        }

        @Test
        @DisplayName("toDomain() préserve un externalId nul → Optional.empty()")
        void toDomain_preservesNullExternalId() {
            var entity = TransactionJpaEntity.of(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("2000"),
                    "XAF",
                    PaymentMethod.WALLET,
                    TransactionStatus.PENDING,
                    "Test",
                    null,  // externalId nul
                    null,
                    NOW, null, null, null
            );

            var domain = TransactionEntityMapper.toDomain(entity);

            assertThat(domain.externalId()).isEmpty();
        }
    }

    // =========================================================================
    // @edge-case — round-trip (toJpaEntity → toDomain)
    // =========================================================================

    @Nested
    @DisplayName("@edge-case — round-trip")
    class RoundTrip {

        @Test
        @DisplayName("toJpaEntity() puis toDomain() préserve tous les champs d'une transaction PENDING")
        void roundTrip_pendingTransaction_preservesAllFields() {
            var original = initiatedTransaction();
            original.pullDomainEvents(); // purge events

            var entity   = TransactionEntityMapper.toJpaEntity(original);
            var restored = TransactionEntityMapper.toDomain(entity);

            assertThat(restored.id()).isEqualTo(original.id());
            assertThat(restored.senderId()).isEqualTo(original.senderId());
            assertThat(restored.recipientId()).isEqualTo(original.recipientId());
            assertThat(restored.amount()).isEqualTo(original.amount());
            assertThat(restored.method()).isEqualTo(original.method());
            assertThat(restored.status()).isEqualTo(original.status());
            assertThat(restored.description()).isEqualTo(original.description());
            assertThat(restored.initiatedAt()).isEqualTo(original.initiatedAt());
        }

        @Test
        @DisplayName("toJpaEntity() puis toDomain() préserve tous les champs d'une transaction COMPLETED")
        void roundTrip_completedTransaction_preservesAllFields() {
            var original = completedTransaction();

            var entity   = TransactionEntityMapper.toJpaEntity(original);
            var restored = TransactionEntityMapper.toDomain(entity);

            assertThat(restored.id()).isEqualTo(original.id());
            assertThat(restored.status()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(restored.externalId()).isEqualTo(original.externalId());
            assertThat(restored.completedAt()).isEqualTo(original.completedAt());
        }

        @Test
        @DisplayName("round-trip préserve une description null")
        void roundTrip_preservesNullDescription() {
            var sender    = UserId.generate();
            var recipient = UserId.generate();
            var original = Transaction.initiate(
                    sender, recipient,
                    Money.of(999L, Currency.XOF),
                    PaymentMethod.MOBILE_MONEY,
                    null,
                    clock
            );

            var restored = TransactionEntityMapper.toDomain(
                    TransactionEntityMapper.toJpaEntity(original)
            );

            assertThat(restored.description()).isEmpty();
        }

        @Test
        @DisplayName("round-trip préserve la devise EUR à l'échelle 2")
        void roundTrip_preservesEurCurrencyScale() {
            var sender    = UserId.generate();
            var recipient = UserId.generate();
            var original = Transaction.initiate(
                    sender, recipient,
                    Money.of(new BigDecimal("99.99"), Currency.EUR),
                    PaymentMethod.CARD,
                    "Achat en ligne",
                    clock
            );

            var restored = TransactionEntityMapper.toDomain(
                    TransactionEntityMapper.toJpaEntity(original)
            );

            assertThat(restored.amount().currency()).isEqualTo(Currency.EUR);
            assertThat(restored.amount().amount()).isEqualByComparingTo("99.99");
        }
    }
}
