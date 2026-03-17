package com.uzima.application.message;

import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.ConversationSummaryPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageMetadata;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/** Use Case : Générer un résumé intelligent d'une conversation. */
public class SummarizeConversationUseCase {

    private static final int MAX_MESSAGES_TO_SUMMARIZE = 100;

    private final ConversationRepositoryPort conversationRepository;
    private final MessageRepositoryPort      messageRepository;
    private final ConversationSummaryPort    summaryPort;

    public SummarizeConversationUseCase(ConversationRepositoryPort conversationRepository,
                                         MessageRepositoryPort messageRepository,
                                         ConversationSummaryPort summaryPort) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository);
        this.messageRepository      = Objects.requireNonNull(messageRepository);
        this.summaryPort            = Objects.requireNonNull(summaryPort);
    }

    /**
     * @param conversationId Identifiant de la conversation à résumer
     * @param requesterId    Utilisateur demandant le résumé (doit être participant)
     * @param language       Langue du résumé souhaité (ex. "fr", "en")
     * @return Texte du résumé
     */
    public String execute(ConversationId conversationId, UserId requesterId, String language) {
        Objects.requireNonNull(conversationId, "conversationId est obligatoire");
        Objects.requireNonNull(requesterId,    "requesterId est obligatoire");
        Objects.requireNonNull(language,       "language est obligatoire");

        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> ResourceNotFoundException.conversationNotFound(conversationId));

        if (!conversation.canSendMessage(requesterId)) {
            throw UnauthorizedException.notConversationParticipant(conversationId);
        }

        List<Message> messages = messageRepository.findByConversationId(
            conversationId, MAX_MESSAGES_TO_SUMMARIZE, 0
        );

        if (messages.isEmpty()) {
            return "Aucun message à résumer.";
        }

        // Formater : "Sender: contenu"  (avec transcription si disponible)
        List<String> formatted = messages.stream()
            .filter(m -> !m.isDeleted())
            .map(m -> {
                String text = m.metadata()
                    .flatMap(MessageMetadata::transcription)
                    .orElse(m.content().text());
                return m.senderId() + ": " + text;
            })
            .toList();

        return summaryPort.summarize(formatted, language);
    }
}
