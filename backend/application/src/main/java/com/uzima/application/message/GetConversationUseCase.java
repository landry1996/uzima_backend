package com.uzima.application.message;

import com.uzima.application.message.port.in.GetConversationQuery;
import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.specification.SenderIsParticipantSpecification;

import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Récupération d'une conversation avec ses messages paginés.
 * <p>
 * Changements vs version initiale :
 * 1. Utilise SenderIsParticipantSpecification au lieu du inline .contains()
 *    → la règle "être participant" est nommée explicitement
 * 2. Hydrate la conversation via Conversation.loadMessage()
 *    → les messages sont accessibles via conversation.recentMessages()
 *    → le ConversationView peut être simplifié
 * 3. Retourne le count total pour la pagination côté client
 */
public final class GetConversationUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final MessageRepositoryPort messageRepository;

    public GetConversationUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository
    ) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository);
        this.messageRepository = Objects.requireNonNull(messageRepository);
    }

    /**
     * Vue paginée d'une conversation.
     *
     * @param conversation La conversation hydratée (avec ses recentMessages)
     * @param totalMessages Nombre total de messages (pour pagination côté client)
     */
    public record ConversationView(Conversation conversation, long totalMessages) {
        /** Délègue à conversation.recentMessages() — plus besoin de les transporter séparément. */
        public List<Message> messages() { return conversation.recentMessages(); }
    }

    public ConversationView execute(GetConversationQuery query) {
        Objects.requireNonNull(query, "La requête ne peut pas être nulle");

        Conversation conversation = conversationRepository.findById(query.conversationId())
                .orElseThrow(() -> new ConversationNotFoundException(
                    "Conversation introuvable : " + query.conversationId()
                ));

        // Vérifier que le demandeur est participant — via SenderIsParticipantSpecification
        // (Justifie l'existence de la Specification, remplace le .contains() inline)
        var participantSpec = new SenderIsParticipantSpecification(query.requesterId());
        if (!participantSpec.isSatisfiedBy(conversation)) {
            throw new UnauthorizedConversationAccessException(
                "Accès non autorisé à la conversation " + query.conversationId()
            );
        }

        // Charger les messages paginés puis les hydrater dans la conversation
        // → Justifie Conversation.loadMessage() et conversation.recentMessages()
        List<Message> messages = messageRepository.findByConversationId(
                query.conversationId(), query.limit(), query.offset()
        );
        messages.forEach(conversation::loadMessage);

        // Compter le total pour la pagination — justifie countByConversationId
        long total = messageRepository.countByConversationId(query.conversationId());

        return new ConversationView(conversation, total);
    }

    public static final class ConversationNotFoundException extends RuntimeException {
        public ConversationNotFoundException(String message) { super(message); }
    }

    /**
     * Étend UnauthorizedException : l'utilisateur n'est pas participant de la conversation.
     * Traduite en HTTP 403 par le GlobalExceptionHandler via handleUnauthorizedConversation().
     * Utilise le constructeur 2-args d'UnauthorizedException avec un errorCode explicite.
     */
    public static final class UnauthorizedConversationAccessException extends UnauthorizedException {
        public UnauthorizedConversationAccessException(String message) {
            super("CONVERSATION_ACCESS_DENIED", message);
        }
    }
}
