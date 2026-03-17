package com.uzima.infrastructure.persistence.message;

import com.uzima.domain.message.model.Message;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'messages'.
 * Infrastructure uniquement. Pas de logique métier ici.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_messages_sent_at", columnList = "sent_at DESC")
})
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MessageJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false, columnDefinition = "uuid")
    private UUID senderId;

    @Column(name = "content", nullable = false, length = 4096)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private Message.MessageType messageType;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Métadonnées IA (nullable)
    @Column(name = "metadata_transcription", columnDefinition = "TEXT")
    private String metadataTranscription;

    @Column(name = "metadata_translation", columnDefinition = "TEXT")
    private String metadataTranslation;

    @Column(name = "metadata_target_language", length = 10)
    private String metadataTargetLanguage;

    @Column(name = "metadata_intent", length = 50)
    private String metadataIntent;

    @Column(name = "metadata_emotion", length = 30)
    private String metadataEmotion;

    public static MessageJpaEntity of(
            UUID id, UUID conversationId, UUID senderId, String content,
            Message.MessageType messageType, Instant sentAt, boolean deleted, Instant deletedAt,
            String metadataTranscription, String metadataTranslation, String metadataTargetLanguage,
            String metadataIntent, String metadataEmotion
    ) {
        MessageJpaEntity e = new MessageJpaEntity();
        e.id                      = id;
        e.conversationId          = conversationId;
        e.senderId                = senderId;
        e.content                 = content;
        e.messageType             = messageType;
        e.sentAt                  = sentAt;
        e.deleted                 = deleted;
        e.deletedAt               = deletedAt;
        e.metadataTranscription   = metadataTranscription;
        e.metadataTranslation     = metadataTranslation;
        e.metadataTargetLanguage  = metadataTargetLanguage;
        e.metadataIntent          = metadataIntent;
        e.metadataEmotion         = metadataEmotion;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageJpaEntity m)) return false;
        return id != null && id.equals(m.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
