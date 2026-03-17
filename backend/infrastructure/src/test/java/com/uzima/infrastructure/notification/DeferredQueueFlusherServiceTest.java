package com.uzima.infrastructure.notification;

import com.uzima.application.notification.PendingNotification;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.domain.message.model.*;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeferredQueueFlusherServiceTest {

    private DeferredNotificationStrategy deferredStrategy;
    private WebSocketNotifierPort notifier;
    private DeferredQueueFlusherService flusher;
    private UserId alice;
    private UserId bob;

    @BeforeEach
    void setUp() {
        deferredStrategy = new DeferredNotificationStrategy();
        notifier = Mockito.mock(WebSocketNotifierPort.class);
        flusher = new DeferredQueueFlusherService(deferredStrategy, notifier);
        alice = UserId.generate();
        bob = UserId.generate();
    }

    @Nested
    @DisplayName("flushForUser()")
    class FlushForUserTests {

        @Test
        @DisplayName("délivre toutes les notifications en attente pour un utilisateur")
        void deliversPendingForUser() {
            PendingNotification n1 = enqueue(alice);
            PendingNotification n2 = enqueue(alice);

            flusher.flushForUser(alice);

            verify(notifier).sendToUser(alice, n1);
            verify(notifier).sendToUser(alice, n2);
            assertThat(deferredStrategy.pendingCount(alice)).isEqualTo(0);
        }

        @Test
        @DisplayName("ne délivre pas les notifications des autres utilisateurs")
        void doesNotFlushOtherUsers() {
            enqueue(alice);
            enqueue(bob);

            flusher.flushForUser(alice);

            verify(notifier, times(1)).sendToUser(eq(alice), any());
            verify(notifier, never()).sendToUser(eq(bob), any());
            assertThat(deferredStrategy.pendingCount(bob)).isEqualTo(1);
        }

        @Test
        @DisplayName("ne fait rien si aucune notification en attente")
        void noopWhenNoPending() {
            flusher.flushForUser(alice);
            verifyNoInteractions(notifier);
        }

        @Test
        @DisplayName("continue la livraison si une notification échoue")
        void continuesOnError() {
            PendingNotification n1 = enqueue(alice);
            PendingNotification n2 = enqueue(alice);
            doThrow(new RuntimeException("erreur réseau")).when(notifier).sendToUser(eq(alice), eq(n1));

            assertThatNoException().isThrownBy(() -> flusher.flushForUser(alice));

            verify(notifier).sendToUser(alice, n2);
        }
    }

    @Nested
    @DisplayName("flushAll()")
    class FlushAllTests {

        @Test
        @DisplayName("délivre les notifications de tous les utilisateurs")
        void deliversForAllUsers() {
            PendingNotification nAlice = enqueue(alice);
            PendingNotification nBob = enqueue(bob);

            flusher.flushAll();

            verify(notifier).sendToUser(alice, nAlice);
            verify(notifier).sendToUser(bob, nBob);
            assertThat(deferredStrategy.totalPendingCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("ne fait rien si toutes les files sont vides")
        void noopWhenAllEmpty() {
            flusher.flushAll();
            verifyNoInteractions(notifier);
        }
    }

    // -------------------------------------------------------------------------

    private PendingNotification enqueue(UserId recipient) {
        Message message = Message.sendText(
                ConversationId.generate(),
                UserId.generate(),
                MessageContent.of("Message test"),
                () -> Instant.parse("2026-03-16T10:00:00Z")
        );
        PendingNotification notification = new PendingNotification(
                message, recipient, PendingNotification.NotificationType.NEW_MESSAGE);
        deferredStrategy.route(notification);
        return notification;
    }
}
