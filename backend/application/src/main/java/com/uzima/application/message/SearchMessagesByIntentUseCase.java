package com.uzima.application.message;

import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Rechercher des messages par intention IA détectée.
 * <p>
 * Exemples : trouver tous les messages "payment_request" dans une conversation.
 */
public class SearchMessagesByIntentUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final MessageRepositoryPort      messageRepository;

    public SearchMessagesByIntentUseCase(ConversationRepositoryPort conversationRepository,
                                          MessageRepositoryPort messageRepository) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository);
        this.messageRepository      = Objects.requireNonNull(messageRepository);
    }

    /**
     * @param conversationId Identifiant de la conversation à fouiller
     * @param requesterId    Utilisateur demandant la recherche (doit être participant)
     * @param intent         Intention à rechercher (ex. "payment_request")
     * @return Messages dont l'intention IA correspond
     */
    public List<Message> execute(ConversationId conversationId, UserId requesterId, String intent) {
        Objects.requireNonNull(conversationId, "conversationId est obligatoire");
        Objects.requireNonNull(requesterId,    "requesterId est obligatoire");
        Objects.requireNonNull(intent,         "intent est obligatoire");

        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> ResourceNotFoundException.conversationNotFound(conversationId));

        if (!conversation.canSendMessage(requesterId)) {
            throw UnauthorizedException.notConversationParticipant(conversationId);
        }

        return messageRepository.findByDetectedIntent(conversationId, intent);
    }
}
