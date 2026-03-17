package com.uzima.infrastructure.persistence.message;

import com.uzima.domain.message.model.Conversation.ConversationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entité JPA : Table 'conversations' + 'conversation_participants'.
 * Infrastructure uniquement. Pas de logique métier.
 */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ConversationJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private ConversationType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "conversation_participants",
            joinColumns = @JoinColumn(name = "conversation_id")
    )
    @Column(name = "user_id", columnDefinition = "uuid")
    private Set<UUID> participantIds = new LinkedHashSet<>();

    public static ConversationJpaEntity of(
            UUID id,
            ConversationType type,
            String title,
            Instant createdAt,
            Set<UUID> participantIds
    ) {
        ConversationJpaEntity e = new ConversationJpaEntity();
        e.id             = id;
        e.type           = type;
        e.title          = title;
        e.createdAt      = createdAt;
        e.participantIds = new LinkedHashSet<>(participantIds);
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationJpaEntity c)) return false;
        return id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
