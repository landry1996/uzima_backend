package com.uzima.application.notification;

import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageContent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du NotificationRouter.
 *
 * Vérifie :
 * - route()       : sélectionne la stratégie selon NotificationPolicy du PresenceStatus
 * - routeUrgent() : bypasse BLOCKED et requalifie en NORMAL pour les urgences
 */
@ExtendWith(MockitoExtension.class)
class NotificationRouterTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> NOW;

    @Mock private NotificationRoutingStrategy normalStrategy;
    @Mock private NotificationRoutingStrategy deferredStrategy;
    @Mock private NotificationRoutingStrategy blockedStrategy;

    private NotificationRouter router;
    private UserId alice;
    private UserId bob;
    private Message message;

    @BeforeEach
    void setUp() {
        when(normalStrategy.supportedPolicy()).thenReturn(PresenceStatus.NotificationPolicy.NORMAL);
        when(deferredStrategy.supportedPolicy()).thenReturn(PresenceStatus.NotificationPolicy.DEFERRED);
        when(blockedStrategy.supportedPolicy()).thenReturn(PresenceStatus.NotificationPolicy.BLOCKED);

        router = new NotificationRouter(
                List.of(normalStrategy, deferredStrategy, blockedStrategy),
                normalStrategy // stratégie par défaut
        );

        alice = UserId.generate();
        bob   = UserId.generate();

        Conversation conv = Conversation.createDirect(alice, bob, clock);
        message = Message.sendText(conv.id(), alice, MessageContent.of("Salut !"), clock);
    }

    // -------------------------------------------------------------------------
    // route()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("NotificationRouter.route()")
    class RouteTest {

        @Test
        @DisplayName("route vers normalStrategy pour AVAILABLE (policy = NORMAL)")
        void routesToNormalStrategyForAvailable() {
            router.route(message, bob, PresenceStatus.AVAILABLE);

            verify(normalStrategy).route(argThat(n ->
                    n.type() == PendingNotification.NotificationType.NEW_MESSAGE));
            verifyNoInteractions(deferredStrategy, blockedStrategy);
        }

        @Test
        @DisplayName("route vers deferredStrategy pour FOCUSED (policy = DEFERRED)")
        void routesToDeferredStrategyForFocused() {
            router.route(message, bob, PresenceStatus.FOCUSED);

            verify(deferredStrategy).route(argThat(n ->
                    n.type() == PendingNotification.NotificationType.NEW_MESSAGE));
            verifyNoInteractions(normalStrategy, blockedStrategy);
        }

        @Test
        @DisplayName("route vers blockedStrategy pour WELLNESS (policy = BLOCKED)")
        void routesToBlockedStrategyForWellness() {
            router.route(message, bob, PresenceStatus.WELLNESS);

            verify(blockedStrategy).route(any());
            verifyNoInteractions(normalStrategy, deferredStrategy);
        }
    }

    // -------------------------------------------------------------------------
    // routeUrgent()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("NotificationRouter.routeUrgent()")
    class RouteUrgentTest {

        @Test
        @DisplayName("routeUrgent() route en URGENT_MESSAGE pour AVAILABLE")
        void routesUrgentForAvailable() {
            router.routeUrgent(message, bob, PresenceStatus.AVAILABLE);

            verify(normalStrategy).route(argThat(n ->
                    n.type() == PendingNotification.NotificationType.URGENT_MESSAGE));
        }

        @Test
        @DisplayName("routeUrgent() bypasse BLOCKED : requalifie en NORMAL même pour WELLNESS")
        void bypassesBlockedForWellness() {
            // WELLNESS a policy = BLOCKED. routeUrgent() bypass → utilise normalStrategy
            router.routeUrgent(message, bob, PresenceStatus.WELLNESS);

            // La stratégie BLOCKED ne doit pas être appelée pour une urgence
            verifyNoInteractions(blockedStrategy);
            // La stratégie NORMAL est utilisée à la place
            verify(normalStrategy).route(argThat(n ->
                    n.type() == PendingNotification.NotificationType.URGENT_MESSAGE));
        }

        @Test
        @DisplayName("routeUrgent() bypasse BLOCKED pour OFFLINE")
        void bypassesBlockedForOffline() {
            router.routeUrgent(message, bob, PresenceStatus.OFFLINE);

            verifyNoInteractions(blockedStrategy);
            verify(normalStrategy).route(argThat(n ->
                    n.type() == PendingNotification.NotificationType.URGENT_MESSAGE));
        }

        @Test
        @DisplayName("routeUrgent() utilise DEFERRED pour FOCUSED (DEFERRED ≠ BLOCKED)")
        void usesDeferredForFocused() {
            // FOCUSED a policy = DEFERRED → routeUrgent ne bypasse que BLOCKED, pas DEFERRED
            router.routeUrgent(message, bob, PresenceStatus.FOCUSED);

            verify(deferredStrategy).route(argThat(n ->
                    n.type() == PendingNotification.NotificationType.URGENT_MESSAGE));
        }
    }
}
