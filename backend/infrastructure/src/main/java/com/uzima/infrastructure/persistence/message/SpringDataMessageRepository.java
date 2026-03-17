package com.uzima.infrastructure.persistence.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository Spring Data JPA interne : messages.
 */
public interface SpringDataMessageRepository extends JpaRepository<MessageJpaEntity, UUID> {

    List<MessageJpaEntity> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);

    long countByConversationIdAndDeletedFalse(UUID conversationId);

    List<MessageJpaEntity> findByConversationIdAndMetadataIntentAndDeletedFalse(
            UUID conversationId, String metadataIntent);
}
