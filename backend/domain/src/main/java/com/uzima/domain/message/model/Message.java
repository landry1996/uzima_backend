package com.uzima.domain.message.model;

import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root : Message.
 * Un message est immuable une fois envoyé (son contenu ne change pas).
 * Il peut être marqué comme supprimé (soft delete).
 * Invariants :
 * - L'expéditeur (senderId) est toujours défini
 * - La conversation est toujours définie
 * - Le contenu est toujours valide (validé par MessageContent VO)
 * - La date d'envoi est toujours définie
 */
public final class Message {

    private final MessageId id;
    private final ConversationId conversationId;
    private final UserId senderId;
    private final MessageContent content;
    private final MessageType type;
    private final Instant sentAt;

    // États mutables
    private boolean         deleted;
    private Instant         deletedAt;
    private MessageMetadata metadata;   // enrichissements IA (nullable)

    private Message(
            MessageId id,
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            MessageType type,
            Instant sentAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.senderId = Objects.requireNonNull(senderId);
        this.content = Objects.requireNonNull(content);
        this.type = Objects.requireNonNull(type);
        this.sentAt = Objects.requireNonNull(sentAt);
        this.deleted = false;
        this.deletedAt = null;
        this.metadata = null;
    }

    /**
     * Factory method : Envoie un nouveau message texte.
     */
    public static Message sendText(
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            TimeProvider clock
    ) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");
        return new Message(
                MessageId.generate(),
                conversationId,
                senderId,
                content,
                MessageType.TEXT,
                clock.now()
        );
    }

    /**
     * Factory method : Reconstitue un message depuis la persistance.
     */
    public static Message reconstitute(
            MessageId id,
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            MessageType type,
            Instant sentAt,
            boolean deleted,
            Instant deletedAt
    ) {
        return reconstitute(id, conversationId, senderId, content, type, sentAt, deleted, deletedAt, null);
    }

    public static Message reconstitute(
            MessageId id,
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            MessageType type,
            Instant sentAt,
            boolean deleted,
            Instant deletedAt,
            MessageMetadata metadata
    ) {
        Message message = new Message(id, conversationId, senderId, content, type, sentAt);
        message.deleted   = deleted;
        message.deletedAt = deletedAt;
        message.metadata  = metadata;
        return message;
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Enrichit le message avec des métadonnées IA.
     * Les métadonnées sont fusionnées si elles existent déjà.
     */
    public void enrich(MessageMetadata newMetadata) {
        Objects.requireNonNull(newMetadata, "Les métadonnées ne peuvent pas être nulles");
        this.metadata = this.metadata == null ? newMetadata : this.metadata.mergeWith(newMetadata);
    }

    /**
     * Supprime le message (soft delete).
     * Seul l'expéditeur peut supprimer son message.
     */
    public void deleteBy(UserId requesterId, TimeProvider clock) {
        Objects.requireNonNull(requesterId, "Le demandeur de suppression ne peut pas être nul");
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        if (this.deleted) {
            throw new MessageAlreadyDeletedException("Le message " + id + " est déjà supprimé");
        }
        if (!this.senderId.equals(requesterId)) {
            throw new UnauthorizedMessageDeletionException(
                "L'utilisateur " + requesterId + " ne peut pas supprimer le message de " + senderId
            );
        }
        this.deleted = true;
        this.deletedAt = clock.now();
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public MessageId id() { return id; }
    public ConversationId conversationId() { return conversationId; }
    public UserId senderId() { return senderId; }
    public MessageContent content() { return content; }
    public MessageType type() { return type; }
    public Instant sentAt() { return sentAt; }
    public boolean isDeleted() { return deleted; }
    public java.util.Optional<Instant> deletedAt() { return java.util.Optional.ofNullable(deletedAt); }
    public java.util.Optional<MessageMetadata> metadata() { return java.util.Optional.ofNullable(metadata); }
    public boolean isVoice() { return type == MessageType.VOICE; }
    public boolean hasTranscription() { return metadata != null && metadata.transcription().isPresent(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message m)) return false;
        return id.equals(m.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // -------------------------------------------------------------------------
    // Types et exceptions
    // -------------------------------------------------------------------------

    public enum MessageType {
        TEXT, VOICE, VIDEO, IMAGE, DOCUMENT, PAYMENT_REQUEST, LOCATION
    }

    public static final class MessageAlreadyDeletedException extends DomainException {
        public MessageAlreadyDeletedException(String message) { super(message); }
    }

    public static final class UnauthorizedMessageDeletionException extends DomainException {
        public UnauthorizedMessageDeletionException(String message) { super(message); }
    }
}
