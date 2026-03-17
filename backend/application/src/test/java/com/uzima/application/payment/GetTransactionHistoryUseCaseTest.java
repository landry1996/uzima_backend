package com.uzima.application.payment;

import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.model.TransactionStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTransactionHistoryUseCase")
class GetTransactionHistoryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");

    @Mock private TransactionRepositoryPort transactionRepository;

    private GetTransactionHistoryUseCase useCase;
    private UserId alice;
    private UserId bob;

    @BeforeEach
    void setUp() {
        useCase = new GetTransactionHistoryUseCase(transactionRepository);
        alice   = UserId.generate();
        bob     = UserId.generate();
    }

    private Transaction buildTransaction(UserId sender, UserId recipient) {
        return Transaction.reconstitute(
            TransactionId.generate(),
            sender, recipient,
            Money.of(new BigDecimal("2000"), Currency.XOF),
            PaymentMethod.MOBILE_MONEY,
            null,
            TransactionStatus.COMPLETED,
            "EXT-001", null,
            NOW, NOW, null, null
        );
    }

    @Nested
    @DisplayName("Résultats corrects")
    class HappyPath {

        @Test
        @DisplayName("retourne les transactions envoyées et reçues")
        void returnsSentAndReceived() {
            Transaction sent     = buildTransaction(alice, bob);
            Transaction received = buildTransaction(bob, alice);

            when(transactionRepository.findBySenderId(eq(alice), anyInt(), anyInt()))
                    .thenReturn(List.of(sent));
            when(transactionRepository.findByRecipientId(eq(alice), anyInt(), anyInt()))
                    .thenReturn(List.of(received));
            when(transactionRepository.countBySenderId(alice)).thenReturn(1L);
            when(transactionRepository.countByRecipientId(alice)).thenReturn(1L);

            var view = useCase.execute(alice, 20, 0);

            assertThat(view.sent()).containsExactly(sent);
            assertThat(view.received()).containsExactly(received);
            assertThat(view.totalSent()).isEqualTo(1L);
            assertThat(view.totalReceived()).isEqualTo(1L);
            assertThat(view.total()).isEqualTo(2L);
        }

        @Test
        @DisplayName("retourne des listes vides si aucune transaction")
        void returnsEmptyListsWhenNoTransactions() {
            when(transactionRepository.findBySenderId(any(), anyInt(), anyInt())).thenReturn(List.of());
            when(transactionRepository.findByRecipientId(any(), anyInt(), anyInt())).thenReturn(List.of());
            when(transactionRepository.countBySenderId(any())).thenReturn(0L);
            when(transactionRepository.countByRecipientId(any())).thenReturn(0L);

            var view = useCase.execute(alice, 20, 0);

            assertThat(view.sent()).isEmpty();
            assertThat(view.received()).isEmpty();
            assertThat(view.total()).isZero();
        }
    }

    @Nested
    @DisplayName("Validation des paramètres")
    class ValidationTest {

        @Test
        @DisplayName("lève NullPointerException si userId nul")
        void throwsWhenUserIdNull() {
            assertThatThrownBy(() -> useCase.execute(null, 20, 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lève IllegalArgumentException si limit <= 0")
        void throwsWhenLimitZero() {
            assertThatThrownBy(() -> useCase.execute(alice, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("limite");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si offset < 0")
        void throwsWhenOffsetNegative() {
            assertThatThrownBy(() -> useCase.execute(alice, 20, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("offset");
        }
    }
}
