package com.uzima.infrastructure.notification;

import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.message.model.*;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

class DeferredNotificationStrategyTest {

    private DeferredNotificationStrategy strategy;
    private UserId alice;
    private UserId bob;

    @BeforeEach
    void setUp() {
        strategy = new DeferredNotificationStrategy();
        alice = UserId.generate();
        bob = UserId.generate();
    }

    @Nested
    @DisplayName("route()")
    class RouteTests {

        @Test
        @DisplayName("met la notification en file")
        void enqueuesNotification() {
            PendingNotification n = notification(alice, PendingNotification.NotificationType.NEW_MESSAGE);
            strategy.route(n);
            assertThat(strategy.pendingCount(alice)).isEqualTo(1);
        }

        @Test
        @DisplayName("accumule plusieurs notifications pour le même utilisateur")
        void accumulatesForSameUser() {
            strategy.route(notification(alice, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(alice, PendingNotification.NotificationType.URGENT_MESSAGE));
            assertThat(strategy.pendingCount(alice)).isEqualTo(2);
        }

        @Test
        @DisplayName("gère plusieurs utilisateurs indépendamment")
        void separatesQueuesPerUser() {
            strategy.route(notification(alice, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(bob, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(bob, PendingNotification.NotificationType.NEW_MESSAGE));
            assertThat(strategy.pendingCount(alice)).isEqualTo(1);
            assertThat(strategy.pendingCount(bob)).isEqualTo(2);
        }

        @Test
        @DisplayName("rejette une notification null")
        void rejectsNull() {
            assertThatNullPointerException().isThrownBy(() -> strategy.route(null));
        }
    }

    @Nested
    @DisplayName("drainQueue()")
    class DrainQueueTests {

        @Test
        @DisplayName("retourne les notifications dans l'ordre FIFO et vide la file")
        void drainsFIFO() {
            PendingNotification n1 = notification(alice, PendingNotification.NotificationType.NEW_MESSAGE);
            PendingNotification n2 = notification(alice, PendingNotification.NotificationType.URGENT_MESSAGE);
            strategy.route(n1);
            strategy.route(n2);

            List<PendingNotification> drained = strategy.drainQueue(alice);

            assertThat(drained).containsExactly(n1, n2);
            assertThat(strategy.pendingCount(alice)).isEqualTo(0);
        }

        @Test
        @DisplayName("retourne une liste vide si aucune notification")
        void returnsEmptyWhenNoPending() {
            assertThat(strategy.drainQueue(alice)).isEmpty();
        }

        @Test
        @DisplayName("le drain est idempotent : deux drains consécutifs, le second est vide")
        void drainIsIdempotent() {
            strategy.route(notification(alice, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.drainQueue(alice);
            assertThat(strategy.drainQueue(alice)).isEmpty();
        }
    }

    @Nested
    @DisplayName("drainAll()")
    class DrainAllTests {

        @Test
        @DisplayName("draine tous les utilisateurs en une seule opération")
        void drainsAllUsers() {
            strategy.route(notification(alice, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(bob, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(bob, PendingNotification.NotificationType.URGENT_MESSAGE));

            Map<UserId, List<PendingNotification>> result = strategy.drainAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(alice)).hasSize(1);
            assertThat(result.get(bob)).hasSize(2);
            assertThat(strategy.totalPendingCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("retourne une map vide si aucune notification en attente")
        void returnsEmptyMapWhenNoPending() {
            assertThat(strategy.drainAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("pendingCount() / totalPendingCount()")
    class CountTests {

        @Test
        @DisplayName("pendingCount retourne 0 pour un utilisateur sans notifications")
        void pendingCountZeroForUnknownUser() {
            assertThat(strategy.pendingCount(alice)).isEqualTo(0);
        }

        @Test
        @DisplayName("totalPendingCount agrège toutes les files")
        void totalPendingCountAggregates() {
            strategy.route(notification(alice, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(bob, PendingNotification.NotificationType.NEW_MESSAGE));
            strategy.route(notification(bob, PendingNotification.NotificationType.NEW_MESSAGE));
            assertThat(strategy.totalPendingCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Thread-safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("aucune perte de notification avec 100 threads concurrent")
        void noPendingLossUnderContention() throws InterruptedException {
            int threads = 100;
            CountDownLatch latch = new CountDownLatch(threads);
            ExecutorService pool = Executors.newFixedThreadPool(threads);

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    strategy.route(notification(alice, PendingNotification.NotificationType.NEW_MESSAGE));
                    latch.countDown();
                });
            }
            latch.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            assertThat(strategy.pendingCount(alice)).isEqualTo(threads);
        }
    }

    // -------------------------------------------------------------------------

    private static PendingNotification notification(UserId recipient,
                                                    PendingNotification.NotificationType type) {
        Message message = Message.sendText(
                ConversationId.generate(),
                UserId.generate(),
                MessageContent.of("Test"),
                () -> Instant.parse("2026-03-16T10:00:00Z")
        );
        return new PendingNotification(message, recipient, type);
    }
}
