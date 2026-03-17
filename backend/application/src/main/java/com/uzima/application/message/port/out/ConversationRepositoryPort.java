package com.uzima.application.message.port.out;

import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie (application) : persistance des conversations.
 */
public interface ConversationRepositoryPort {
    void save(Conversation conversation);
    Optional<Conversation> findById(ConversationId id);
    Optional<Conversation> findDirectConversation(UserId userA, UserId userB);
    List<Conversation> findByParticipant(UserId userId);
}
