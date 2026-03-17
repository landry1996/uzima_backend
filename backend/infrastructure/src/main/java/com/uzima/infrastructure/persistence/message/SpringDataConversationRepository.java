package com.uzima.infrastructure.persistence.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataConversationRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    /**
     * Trouve la conversation DIRECTE entre deux utilisateurs précis.
     * Utilise MEMBER OF sur l'ElementCollection participantIds.
     */
    @Query("""
            SELECT c FROM ConversationJpaEntity c
            WHERE c.type = 'DIRECT'
              AND :userAId MEMBER OF c.participantIds
              AND :userBId MEMBER OF c.participantIds
            """)
    Optional<ConversationJpaEntity> findDirectConversation(
            @Param("userAId") UUID userAId,
            @Param("userBId") UUID userBId
    );

    /**
     * Toutes les conversations auxquelles un utilisateur participe.
     */
    @Query("""
            SELECT DISTINCT c FROM ConversationJpaEntity c
            WHERE :userId MEMBER OF c.participantIds
            ORDER BY c.createdAt DESC
            """)
    List<ConversationJpaEntity> findByParticipant(@Param("userId") UUID userId);
}
