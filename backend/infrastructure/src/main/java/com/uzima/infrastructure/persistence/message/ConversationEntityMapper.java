package com.uzima.infrastructure.persistence.message;

import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.user.model.UserId;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper : Conversion bidirectionnelle entre Conversation (domaine) et ConversationJpaEntity.
 */
public final class ConversationEntityMapper {

    private ConversationEntityMapper() {}

    public static ConversationJpaEntity toJpaEntity(Conversation conversation) {
        Set<java.util.UUID> participantIds = conversation.participants().stream()
                .map(UserId::value)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return ConversationJpaEntity.of(
                conversation.id().value(),
                conversation.type(),
                conversation.title().orElse(null),
                conversation.createdAt(),
                participantIds
        );
    }

    public static Conversation toDomain(ConversationJpaEntity entity) {
        Set<UserId> participants = entity.getParticipantIds().stream()
                .map(UserId::of)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Conversation.reconstitute(
                ConversationId.of(entity.getId()),
                entity.getType(),
                participants,
                entity.getTitle(),
                entity.getCreatedAt()
        );
    }
}
