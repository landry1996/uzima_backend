package com.uzima.application.message;

import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Récupérer toutes les conversations d'un utilisateur.
 * <p>
 * C'est la "boîte de réception" de l'utilisateur : toutes les conversations
 * directes et de groupe dans lesquelles il participe, triées par dernier message.
 * <p>
 * Justifie findByParticipant dans ConversationRepository.
 * Pas de Spring. Pas de framework.
 */
public final class GetUserConversationsUseCase {

    private final ConversationRepositoryPort conversationRepository;

    public GetUserConversationsUseCase(ConversationRepositoryPort conversationRepository) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository);
    }

    /**
     * @param userId L'identifiant de l'utilisateur dont on veut la liste de conversations
     * @return Toutes les conversations de l'utilisateur, triées par activité récente
     */
    public List<Conversation> execute(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        return conversationRepository.findByParticipant(userId);
    }
}
