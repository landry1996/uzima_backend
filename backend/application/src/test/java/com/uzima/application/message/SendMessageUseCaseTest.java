package com.uzima.application.message;

import com.uzima.application.message.port.in.SendMessageCommand;
import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.MessageNotificationPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendMessageUseCaseTest {

    @Mock private ConversationRepositoryPort conversationRepository;
    @Mock private MessageRepositoryPort messageRepository;
    @Mock private MessageNotificationPort notificationPort;
    @Mock private UserRepositoryPort userRepository;
    @Mock private User bobUser;

    private final TimeProvider clock = () -> Instant.parse("2026-03-12T10:00:00Z");

    private SendMessageUseCase useCase;
    private UserId alice;
    private UserId bob;
    private Conversation directConversation;

    @BeforeEach
    void setUp() {
        // Constructeur complet : 5 paramètres (conversationRepository, messageRepository,
        // notificationPort, userRepository, clock)
        useCase = new SendMessageUseCase(
                conversationRepository, messageRepository, notificationPort, userRepository, clock);
        alice = UserId.generate();
        bob   = UserId.generate();
        directConversation = Conversation.createDirect(alice, bob, clock);
    }

    @Nested
    @DisplayName("Envoi réussi — message texte (SendMessageCommand.text())")
    class HappyPathText {

        @BeforeEach
        void setUp() {
            when(conversationRepository.findById(directConversation.id()))
                    .thenReturn(Optional.of(directConversation));
        }

        @Test
        @DisplayName("retourne le message créé — via SendMessageCommand.text()")
        void returnsCreatedMessage() {
            // SendMessageCommand.text() : constructeur de convenance pour les messages texte
            var command = SendMessageCommand.text(directConversation.id(), alice, "Salut Bob !");

            Message result = useCase.execute(command);

            assertThat(result).isNotNull();
            assertThat(result.senderId()).isEqualTo(alice);
            assertThat(result.content().text()).isEqualTo("Salut Bob !");
        }

        @Test
        @DisplayName("persiste le message")
        void savesMessage() {
            var command = SendMessageCommand.text(directConversation.id(), alice, "Salut Bob !");
            useCase.execute(command);
            verify(messageRepository, times(1)).save(any(Message.class));
        }

        @Test
        @DisplayName("notifie les autres participants")
        void notifiesOtherParticipants() {
            var command = SendMessageCommand.text(directConversation.id(), alice, "Salut Bob !");
            useCase.execute(command);
            verify(notificationPort, times(1)).notifyNewMessage(any(Message.class), eq(Set.of(bob)));
        }

        @Test
        @DisplayName("n'inclut pas l'expéditeur dans les destinataires de notification")
        void doesNotNotifySender() {
            var command = SendMessageCommand.text(directConversation.id(), alice, "Salut Bob !");
            useCase.execute(command);
            verify(notificationPort).notifyNewMessage(any(), argThat(recipients -> !recipients.contains(alice)));
        }
    }

    @Nested
    @DisplayName("Envoi réussi — message vocal (SendMessageCommand.voice())")
    class HappyPathVoice {

        @BeforeEach
        void setUp() {
            when(conversationRepository.findById(directConversation.id()))
                    .thenReturn(Optional.of(directConversation));
            // Bob accepte les messages vocaux (AVAILABLE)
            when(userRepository.findById(bob)).thenReturn(Optional.of(bobUser));
            when(bobUser.presenceStatus()).thenReturn(PresenceStatus.AVAILABLE);
        }

        @Test
        @DisplayName("envoie un message vocal si le destinataire accepte — via SendMessageCommand.voice()")
        void sendsVoiceMessageWhenRecipientAccepts() {
            // SendMessageCommand.voice() : constructeur de convenance avec isVoiceMessage=true
            var command = SendMessageCommand.voice(
                    directConversation.id(), alice, "https://cdn.uzima.app/audio/msg-001.ogg");

            assertThat(command.isVoiceMessage()).isTrue();

            Message result = useCase.execute(command);

            assertThat(result).isNotNull();
            verify(messageRepository, times(1)).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("Messages vocaux refusés")
    class VoiceMessageRejected {

        @BeforeEach
        void setUp() {
            when(conversationRepository.findById(directConversation.id()))
                    .thenReturn(Optional.of(directConversation));
        }

        @Test
        @DisplayName("lève VoiceMessageNotAllowedException si le destinataire est en SILENCE")
        void rejectsVoiceWhenRecipientIsSilent() {
            when(userRepository.findById(bob)).thenReturn(Optional.of(bobUser));
            when(bobUser.presenceStatus()).thenReturn(PresenceStatus.SILENCE);

            var command = SendMessageCommand.voice(
                    directConversation.id(), alice, "https://cdn.uzima.app/audio/msg.ogg");

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(SendMessageUseCase.VoiceMessageNotAllowedException.class);

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève VoiceMessageNotAllowedException si le destinataire est en WELLNESS")
        void rejectsVoiceWhenRecipientIsInWellness() {
            when(userRepository.findById(bob)).thenReturn(Optional.of(bobUser));
            when(bobUser.presenceStatus()).thenReturn(PresenceStatus.WELLNESS);

            var command = SendMessageCommand.voice(
                    directConversation.id(), alice, "https://cdn.uzima.app/audio/msg.ogg");

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(SendMessageUseCase.VoiceMessageNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCases {

        @Test
        @DisplayName("lève une exception si la conversation n'existe pas")
        void throwsWhenConversationNotFound() {
            when(conversationRepository.findById(any())).thenReturn(Optional.empty());
            var command = SendMessageCommand.text(directConversation.id(), alice, "Salut");

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(SendMessageUseCase.ConversationNotFoundException.class);
        }

        @Test
        @DisplayName("lève SenderNotInConversationException (extends UnauthorizedException) si non participant")
        void throwsWhenSenderNotParticipant() {
            UserId charlie = UserId.generate();
            when(conversationRepository.findById(directConversation.id()))
                    .thenReturn(Optional.of(directConversation));

            var command = SendMessageCommand.text(directConversation.id(), charlie, "Intrusion");

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(SendMessageUseCase.SenderNotInConversationException.class);
        }

        @Test
        @DisplayName("ne persiste pas si l'expéditeur n'est pas participant")
        void doesNotSaveWhenUnauthorized() {
            UserId charlie = UserId.generate();
            when(conversationRepository.findById(directConversation.id()))
                    .thenReturn(Optional.of(directConversation));

            try {
                useCase.execute(SendMessageCommand.text(directConversation.id(), charlie, "Intrusion"));
            } catch (Exception ignored) {}

            verify(messageRepository, never()).save(any());
        }
    }
}
