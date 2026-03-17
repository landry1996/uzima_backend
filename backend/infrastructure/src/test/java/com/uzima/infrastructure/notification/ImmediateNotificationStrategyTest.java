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

class ImmediateNotificationStrategyTest {

    private WebSocketNotifierPort notifier;
    private ImmediateNotificationStrategy strategy;

    @BeforeEach
    void setUp() {
        notifier = Mockito.mock(WebSocketNotifierPort.class);
        strategy = new ImmediateNotificationStrategy(notifier);
    }

    @Test
    @DisplayName("délègue sendToUser au port WebSocket")
    void delegatesToWebSocketPort() {
        UserId recipient = UserId.generate();
        Message message = buildMessage(recipient);
        PendingNotification notification = new PendingNotification(
                message, recipient, PendingNotification.NotificationType.NEW_MESSAGE);

        strategy.route(notification);

        verify(notifier, times(1)).sendToUser(recipient, notification);
    }

    @Test
    @DisplayName("propage l'exception levée par le port")
    void propagatesPortException() {
        UserId recipient = UserId.generate();
        Message message = buildMessage(recipient);
        PendingNotification notification = new PendingNotification(
                message, recipient, PendingNotification.NotificationType.NEW_MESSAGE);
        doThrow(new RuntimeException("WebSocket down")).when(notifier).sendToUser(any(), any());

        assertThatThrownBy(() -> strategy.route(notification))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("WebSocket down");
    }

    @Test
    @DisplayName("rejette une notification null")
    void rejectsNullNotification() {
        assertThatNullPointerException().isThrownBy(() -> strategy.route(null));
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("supporte la policy NORMAL")
    void supportsPolicyNormal() {
        assertThat(strategy.supportedPolicy())
                .isEqualTo(com.uzima.domain.user.model.PresenceStatus.NotificationPolicy.NORMAL);
    }

    // -------------------------------------------------------------------------

    private static Message buildMessage(UserId senderId) {
        return Message.sendText(
                ConversationId.generate(),
                senderId,
                MessageContent.of("Bonjour !"),
                () -> Instant.parse("2026-03-16T10:00:00Z")
        );
    }
}
