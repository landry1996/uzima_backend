package com.uzima.application.payment;

import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.model.TransactionStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de CancelTransactionUseCase.
 *
 * Pas de Spring. Mocks Mockito pour les ports.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelTransactionUseCase")
class CancelTransactionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private final TimeProvider clock = () -> NOW;

    @Mock private TransactionRepositoryPort transactionRepository;

    private CancelTransactionUseCase useCase;

    private UserId sender;
    private UserId recipient;

    @BeforeEach
    void setUp() {
        useCase   = new CancelTransactionUseCase(transactionRepository, clock);
        sender    = UserId.generate();
        recipient = UserId.generate();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Transaction pendingTransaction(TransactionId id) {
        return Transaction.reconstitute(
                id, sender, recipient,
                Money.of(new BigDecimal("5000"), Currency.XAF),
                PaymentMethod.MOBILE_MONEY,
                "Test annulation",
                TransactionStatus.PENDING,
                null, null,
                NOW, null, null, null
        );
    }

    private Transaction completedTransaction(TransactionId id) {
        return Transaction.reconstitute(
                id, sender, recipient,
                Money.of(new BigDecimal("5000"), Currency.XAF),
                PaymentMethod.MOBILE_MONEY,
                "Test",
                TransactionStatus.COMPLETED,
                "EXT-001", null,
                NOW, NOW, null, null
        );
    }

    // =========================================================================
    // @positive — annulation réussie
    // =========================================================================

    @Nested
    @DisplayName("@positive — annulation réussie")
    class Success {

        @Test
        @DisplayName("execute() annule une transaction PENDING et la sauvegarde")
        void execute_pendingTransaction_cancelsAndSaves() {
            var txId = TransactionId.generate();
            var tx   = pendingTransaction(txId);

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            useCase.execute(txId, sender);

            verify(transactionRepository, times(1)).save(tx);
            assertThat(tx.status()).isEqualTo(TransactionStatus.CANCELLED);
        }

        @Test
        @DisplayName("execute() appelle cancel() sur la transaction")
        void execute_callsCancelOnTransaction() {
            var txId = TransactionId.generate();
            var tx   = pendingTransaction(txId);

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            useCase.execute(txId, sender);

            assertThat(tx.canBeCancelled()).isFalse(); // cancel() a bien été appelé
            assertThat(tx.cancelledAt()).isPresent();
        }
    }

    // =========================================================================
    // @negative — transaction introuvable
    // =========================================================================

    @Nested
    @DisplayName("@negative — transaction introuvable")
    class NotFound {

        @Test
        @DisplayName("execute() lance ResourceNotFoundException si la transaction n'existe pas")
        void execute_nonExistentTransaction_throwsResourceNotFound() {
            var txId = TransactionId.generate();
            when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(txId, sender))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("execute() ne sauvegarde rien si la transaction est introuvable")
        void execute_nonExistentTransaction_neverSaves() {
            var txId = TransactionId.generate();
            when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(txId, sender))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // @negative — accès refusé (mauvais demandeur)
    // =========================================================================

    @Nested
    @DisplayName("@negative — accès refusé")
    class AccessDenied {

        @Test
        @DisplayName("execute() lance TransactionAccessDeniedException si le demandeur n'est pas l'expéditeur")
        void execute_wrongRequester_throwsAccessDeniedException() {
            var txId         = TransactionId.generate();
            var tx           = pendingTransaction(txId);
            var otherUser    = UserId.generate(); // pas le sender

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> useCase.execute(txId, otherUser))
                    .isInstanceOf(CancelTransactionUseCase.TransactionAccessDeniedException.class);
        }

        @Test
        @DisplayName("execute() ne sauvegarde rien si l'accès est refusé")
        void execute_wrongRequester_neverSaves() {
            var txId      = TransactionId.generate();
            var tx        = pendingTransaction(txId);
            var otherUser = UserId.generate();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> useCase.execute(txId, otherUser))
                    .isInstanceOf(CancelTransactionUseCase.TransactionAccessDeniedException.class);

            verify(transactionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // @negative — transition invalide
    // =========================================================================

    @Nested
    @DisplayName("@negative — transition invalide")
    class IllegalTransition {

        @Test
        @DisplayName("execute() lance IllegalTransitionException si la transaction est déjà COMPLETED")
        void execute_completedTransaction_throwsIllegalTransition() {
            var txId = TransactionId.generate();
            var tx   = completedTransaction(txId);

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> useCase.execute(txId, sender))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }

        @Test
        @DisplayName("execute() ne sauvegarde rien si la transition est invalide")
        void execute_completedTransaction_neverSaves() {
            var txId = TransactionId.generate();
            var tx   = completedTransaction(txId);

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> useCase.execute(txId, sender))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("execute() lance IllegalTransitionException si la transaction est déjà CANCELLED")
        void execute_alreadyCancelledTransaction_throwsIllegalTransition() {
            var txId = TransactionId.generate();
            var tx = Transaction.reconstitute(
                    txId, sender, recipient,
                    Money.of(1000L, Currency.XOF),
                    PaymentMethod.WALLET,
                    null,
                    TransactionStatus.CANCELLED,
                    null, null,
                    NOW, null, null, NOW
            );

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> useCase.execute(txId, sender))
                    .isInstanceOf(Transaction.IllegalTransitionException.class);
        }
    }
}
