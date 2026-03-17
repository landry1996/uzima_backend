package com.uzima.infrastructure.notification;

import com.uzima.application.notification.PendingNotification;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.domain.message.model.*;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UrgentOnlyNotificationStrategyTest {

    private WebSocketNotifierPort notifier;
    private ImmediateNotificationStrategy immediate;
    private DeferredNotificationStrategy deferred;
    private UrgentOnlyNotificationStrategy strategy;
    private UserId recipient;

    @BeforeEach
    void setUp() {
        notifier = Mockito.mock(WebSocketNotifierPort.class);
        immediate = new ImmediateNotificationStrategy(notifier);
        deferred = new DeferredNotificationStrategy();
        strategy = new UrgentOnlyNotificationStrategy(immediate, deferred);
        recipient = UserId.generate();
    }

    @Test
    @DisplayName("notification URGENTE → envoi WebSocket immédiat")
    void urgentGoesToImmediate() {
        PendingNotification urgent = notification(PendingNotification.NotificationType.URGENT_MESSAGE);

        strategy.route(urgent);

        verify(notifier, times(1)).sendToUser(recipient, urgent);
        assertThat(deferred.pendingCount(recipient)).isEqualTo(0);
    }

    @Test
    @DisplayName("notification NORMALE → mise en file différée")
    void normalGoesToDeferred() {
        PendingNotification normal = notification(PendingNotification.NotificationType.NEW_MESSAGE);

        strategy.route(normal);

        verifyNoInteractions(notifier);
        assertThat(deferred.pendingCount(recipient)).isEqualTo(1);
    }

    @Test
    @DisplayName("rejette une notification null")
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> strategy.route(null));
    }

    @Test
    @DisplayName("supporte la policy URGENT_ONLY")
    void supportsPolicyUrgentOnly() {
        assertThat(strategy.supportedPolicy())
                .isEqualTo(com.uzima.domain.user.model.PresenceStatus.NotificationPolicy.URGENT_ONLY);
    }

    // -------------------------------------------------------------------------

    private PendingNotification notification(PendingNotification.NotificationType type) {
        Message message = Message.sendText(
                ConversationId.generate(),
                UserId.generate(),
                MessageContent.of("Test urgent"),
                () -> Instant.parse("2026-03-16T10:00:00Z")
        );
        return new PendingNotification(message, recipient, type);
    }
}
