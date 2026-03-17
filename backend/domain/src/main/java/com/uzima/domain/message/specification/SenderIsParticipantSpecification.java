package com.uzima.domain.message.specification;

import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.shared.specification.Specification;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Specification : vérifie qu'un utilisateur est participant d'une conversation.
 * Remplace le test inline dans SendMessageUseCase et GetConversationUseCase.
 * Testable indépendamment, réutilisable dans tous les use cases de messagerie.
 * Justification du pattern : cette règle ("l'expéditeur doit être dans la conversation")
 * est vérifiée à plusieurs endroits (envoi, lecture, suppression). La centraliser
 * dans une Specification évite la duplication et garantit la cohérence.
 */
public final class SenderIsParticipantSpecification implements Specification<Conversation> {

    private final UserId userId;

    public SenderIsParticipantSpecification(UserId userId) {
        this.userId = Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
    }

    @Override
    public boolean isSatisfiedBy(Conversation conversation) {
        return conversation.canSendMessage(userId);
    }
}
