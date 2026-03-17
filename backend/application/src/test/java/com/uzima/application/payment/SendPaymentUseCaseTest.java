package com.uzima.application.payment;

import com.uzima.application.payment.port.in.SendPaymentCommand;
import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.model.TransactionStatus;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de SendPaymentUseCase.
 *
 * Pas de Spring. Mocks Mockito pour les ports.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendPaymentUseCase")
class SendPaymentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> NOW;

    @Mock private TransactionRepositoryPort transactionRepository;
    @Mock private PaymentGatewayPort paymentGateway;

    private SendPaymentUseCase useCase;
    private UserId alice;
    private UserId bob;
    private SendPaymentCommand command;

    @BeforeEach
    void setUp() {
        useCase = new SendPaymentUseCase(transactionRepository, paymentGateway, clock);
        alice   = UserId.generate();
        bob     = UserId.generate();
        command = new SendPaymentCommand(
            alice, bob,
            new BigDecimal("5000"), Currency.XOF,
            PaymentMethod.MOBILE_MONEY, "Remboursement"
        );
    }

    // -------------------------------------------------------------------------
    // Cas nominal
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Paiement réussi")
    class HappyPath {

        @Test
        @DisplayName("retourne un TransactionId après succès gateway")
        void returnsTransactionIdOnSuccess() {
            when(paymentGateway.process(any()))
                    .thenReturn(PaymentGatewayPort.GatewayResponse.success("EXT-001"));

            TransactionId id = useCase.execute(command);

            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("sauvegarde la transaction en PENDING avant l'appel gateway")
        void savesPendingBeforeGatewayCall() {
            when(paymentGateway.process(any()))
                    .thenReturn(PaymentGatewayPort.GatewayResponse.success("EXT-001"));

            useCase.execute(command);

            // Premier save = PENDING, second save = COMPLETED
            verify(transactionRepository, times(2)).save(any(Transaction.class));

            // Vérifier que le premier save est bien en PENDING
            var inOrder = inOrder(transactionRepository, paymentGateway);
            inOrder.verify(transactionRepository).save(argThat(tx ->
                    tx.status() == TransactionStatus.PENDING));
            inOrder.verify(paymentGateway).process(any());
            inOrder.verify(transactionRepository).save(argThat(tx ->
                    tx.status() == TransactionStatus.COMPLETED));
        }

        @Test
        @DisplayName("la transaction finale est COMPLETED avec l'externalId")
        void transactionIsCompletedWithExternalId() {
            when(paymentGateway.process(any()))
                    .thenReturn(PaymentGatewayPort.GatewayResponse.success("EXT-XYZ"));

            useCase.execute(command);

            verify(transactionRepository).save(argThat(tx ->
                    tx.status() == TransactionStatus.COMPLETED
                    && tx.externalId().contains("EXT-XYZ")));
        }
    }

    // -------------------------------------------------------------------------
    // Échec gateway
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Échec gateway")
    class GatewayFailure {

        @Test
        @DisplayName("sauvegarde la transaction en FAILED si la gateway refuse")
        void savesFailedTransactionWhenGatewayRefuses() {
            when(paymentGateway.process(any()))
                    .thenReturn(PaymentGatewayPort.GatewayResponse.failure("Fonds insuffisants"));

            useCase.execute(command);

            verify(transactionRepository).save(argThat(tx ->
                    tx.status() == TransactionStatus.FAILED
                    && tx.failureReason().contains("Fonds insuffisants")));
        }

        @Test
        @DisplayName("lève PaymentGatewayException si la gateway lève une exception")
        void throwsPaymentGatewayExceptionOnTechnicalError() {
            when(paymentGateway.process(any()))
                    .thenThrow(new RuntimeException("Timeout réseau"));

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(SendPaymentUseCase.PaymentGatewayException.class)
                    .hasMessageContaining("temporairement indisponible");
        }

        @Test
        @DisplayName("sauvegarde la transaction en FAILED si la gateway est inaccessible")
        void savesFailedTransactionOnGatewayException() {
            when(paymentGateway.process(any()))
                    .thenThrow(new RuntimeException("Timeout réseau"));

            try { useCase.execute(command); } catch (Exception ignored) {}

            // La transaction doit être sauvegardée en FAILED même en cas d'exception technique
            verify(transactionRepository).save(argThat(tx ->
                    tx.status() == TransactionStatus.FAILED));
        }

        @Test
        @DisplayName("ne sauvegarde pas une troisième fois si la gateway échoue normalement")
        void savesExactlyTwiceOnGatewayRefusal() {
            when(paymentGateway.process(any()))
                    .thenReturn(PaymentGatewayPort.GatewayResponse.failure("Carte refusée"));

            useCase.execute(command);

            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }
    }

    // -------------------------------------------------------------------------
    // Invariants domaine propagés
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Invariants domaine")
    class DomainInvariants {

        @Test
        @DisplayName("lève SelfPaymentException si sender == recipient")
        void throwsOnSelfPayment() {
            var selfCommand = new SendPaymentCommand(
                alice, alice,
                new BigDecimal("1000"), Currency.XOF,
                PaymentMethod.WALLET, null
            );

            assertThatThrownBy(() -> useCase.execute(selfCommand))
                    .isInstanceOf(Transaction.SelfPaymentException.class);

            verifyNoInteractions(paymentGateway);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("lève NonPositiveAmountException si montant = 0")
        void throwsOnZeroAmount() {
            var zeroCommand = new SendPaymentCommand(
                alice, bob,
                BigDecimal.ZERO, Currency.XOF,
                PaymentMethod.WALLET, null
            );

            assertThatThrownBy(() -> useCase.execute(zeroCommand))
                    .isInstanceOf(Transaction.NonPositiveAmountException.class);

            verifyNoInteractions(paymentGateway);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("lève NegativeAmountException si montant < 0")
        void throwsOnNegativeAmount() {
            var negativeCommand = new SendPaymentCommand(
                alice, bob,
                new BigDecimal("-100"), Currency.XOF,
                PaymentMethod.WALLET, null
            );

            assertThatThrownBy(() -> useCase.execute(negativeCommand))
                    .isInstanceOf(Money.NegativeAmountException.class);

            verifyNoInteractions(paymentGateway);
        }
    }
}
