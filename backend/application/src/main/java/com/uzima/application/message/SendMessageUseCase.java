package com.uzima.application.message;

import com.uzima.application.message.port.in.SendMessageCommand;
import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.MessageNotificationPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageContent;
import com.uzima.domain.message.specification.SenderIsParticipantSpecification;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use Case : Envoi d'un message dans une conversation.
 * <p>
 * Changements vs version initiale :
 * 1. Utilise SenderIsParticipantSpecification — Justifie la Specification
 * 2. Vérifie allowsVoiceMessages() pour les messages vocaux — Justifie la méthode PresenceStatus
 * <p>
 * Orchestration :
 * 1. Charger la conversation
 * 2. Vérifier que l'expéditeur est participant (via Specification)
 * 3. Pour les messages vocaux : vérifier que les destinataires l'acceptent
 * 4. Créer le message (logique domaine)
 * 5. Persister
 * 6. Notifier les autres participants
 */
public final class SendMessageUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final MessageRepositoryPort messageRepository;
    private final MessageNotificationPort notificationPort;
    private final UserRepositoryPort userRepository;
    private final TimeProvider clock;

    public SendMessageUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository,
            MessageNotificationPort notificationPort,
            UserRepositoryPort userRepository,
            TimeProvider clock
    ) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository);
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.notificationPort = Objects.requireNonNull(notificationPort);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Message execute(SendMessageCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        // 1. Charger la conversation
        Conversation conversation = conversationRepository.findById(command.conversationId())
                .orElseThrow(() -> new ConversationNotFoundException(
                    "Conversation introuvable : " + command.conversationId()
                ));

        // 2. Vérifier via Specification (remplace le if !canSendMessage inline)
        //    → Justifie SenderIsParticipantSpecification
        var participantSpec = new SenderIsParticipantSpecification(command.senderId());
        if (!participantSpec.isSatisfiedBy(conversation)) {
            throw new SenderNotInConversationException(
                "L'utilisateur " + command.senderId() + " n'est pas participant de la conversation"
            );
        }

        // 3. Pour les messages vocaux : vérifier que les destinataires l'acceptent
        //    → Justifie PresenceStatus.allowsVoiceMessages()
        if (command.isVoiceMessage()) {
            validateVoiceMessageRecipients(conversation, command.senderId());
        }

        // 4. Créer le message (validation du contenu déléguée au VO)
        MessageContent content = MessageContent.of(command.textContent());
        Message message = Message.sendText(conversation.id(), command.senderId(), content, clock);

        // 5. Persister le message
        messageRepository.save(message);

        // 6. Notifier les autres participants
        Set<UserId> recipients = conversation.participants().stream()
                .filter(p -> !p.equals(command.senderId()))
                .collect(Collectors.toUnmodifiableSet());
        notificationPort.notifyNewMessage(message, recipients);

        return message;
    }

    /**
     * Vérifie que tous les destinataires acceptent les messages vocaux.
     * Si au moins un destinataire est en état SILENCE, WELLNESS ou SLEEPING,
     * on lève une exception — l'expéditeur doit envoyer un message texte.
     * <p>
     * Règle métier : respect du contexte de vie numérique (Nouveauté 6).
     * → Justifie PresenceStatus.allowsVoiceMessages()
     */
    private void validateVoiceMessageRecipients(Conversation conversation, UserId senderId) {
        conversation.participants().stream()
                .filter(p -> !p.equals(senderId))
                .forEach(recipientId -> {
                    PresenceStatus status = userRepository.findById(recipientId)
                            .map(User::presenceStatus)
                            .orElse(PresenceStatus.OFFLINE);
                    if (!status.allowsVoiceMessages()) {
                        throw new VoiceMessageNotAllowedException(
                            "Le destinataire " + recipientId +
                            " est en état '" + status.displayName() +
                            "' et n'accepte pas les messages vocaux"
                        );
                    }
                });
    }

    public static final class ConversationNotFoundException extends RuntimeException {
        public ConversationNotFoundException(String message) { super(message); }
    }

    /**
     * Étend UnauthorizedException : l'expéditeur n'est pas autorisé à écrire dans cette conversation.
     * Traduite en HTTP 403 par le GlobalExceptionHandler via handleSenderNotInConversation().
     * Utilise le constructeur 1-arg d'UnauthorizedException (code par défaut : UNAUTHORIZED_ACCESS).
     */
    public static final class SenderNotInConversationException extends UnauthorizedException {
        public SenderNotInConversationException(String message) { super(message); }
    }

    /**
     * Levée quand un message vocal est envoyé à un utilisateur en SILENCE/WELLNESS/SLEEPING.
     * → Justifie PresenceStatus.allowsVoiceMessages() et displayName()
     */
    public static final class VoiceMessageNotAllowedException extends RuntimeException {
        public VoiceMessageNotAllowedException(String message) { super(message); }
    }
}
