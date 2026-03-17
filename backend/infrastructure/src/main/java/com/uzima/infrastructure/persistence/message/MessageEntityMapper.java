package com.uzima.infrastructure.persistence.message;

import com.uzima.domain.message.model.*;
import com.uzima.domain.user.model.UserId;

import java.util.Optional;

/**
 * Mapper : Conversion bidirectionnelle entre Message (domaine) et MessageJpaEntity (infrastructure).
 */
public final class MessageEntityMapper {

    private MessageEntityMapper() {}

    public static MessageJpaEntity toJpaEntity(Message message) {
        Optional<MessageMetadata> meta = message.metadata();
        return MessageJpaEntity.of(
                message.id().value(),
                message.conversationId().value(),
                message.senderId().value(),
                message.content().text(),
                message.type(),
                message.sentAt(),
                message.isDeleted(),
                message.deletedAt().orElse(null),
                meta.flatMap(MessageMetadata::transcription).orElse(null),
                meta.flatMap(MessageMetadata::translation).orElse(null),
                meta.flatMap(MessageMetadata::targetLanguage).orElse(null),
                meta.flatMap(MessageMetadata::detectedIntent).orElse(null),
                meta.flatMap(MessageMetadata::detectedEmotion).orElse(null)
        );
    }

    public static Message toDomain(MessageJpaEntity entity) {
        MessageMetadata metadata = null;
        if (entity.getMetadataTranscription() != null
                || entity.getMetadataTranslation() != null
                || entity.getMetadataIntent() != null
                || entity.getMetadataEmotion() != null) {
            metadata = new MessageMetadata(
                entity.getMetadataTranscription(),
                entity.getMetadataTranslation(),
                entity.getMetadataTargetLanguage(),
                entity.getMetadataIntent(),
                entity.getMetadataEmotion()
            );
        }
        return Message.reconstitute(
                MessageId.of(entity.getId()),
                ConversationId.of(entity.getConversationId()),
                UserId.of(entity.getSenderId()),
                MessageContent.of(entity.getContent()),
                entity.getMessageType(),
                entity.getSentAt(),
                entity.isDeleted(),
                entity.getDeletedAt(),
                metadata
        );
    }
}
