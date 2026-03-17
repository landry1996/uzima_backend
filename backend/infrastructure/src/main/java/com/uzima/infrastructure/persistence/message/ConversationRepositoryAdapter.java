package com.uzima.infrastructure.persistence.message;

import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.user.model.UserId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente ConversationRepositoryPort avec JPA.
 * Délègue le mapping à ConversationEntityMapper.
 * Enveloppe les exceptions JPA en DatabaseException.
 */
public final class ConversationRepositoryAdapter implements ConversationRepositoryPort {

    private final SpringDataConversationRepository jpaRepository;

    public ConversationRepositoryAdapter(SpringDataConversationRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(Conversation conversation) {
        try {
            jpaRepository.save(ConversationEntityMapper.toJpaEntity(conversation));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde de la conversation", ex);
        }
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(ConversationEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de la conversation", ex);
        }
    }

    @Override
    public Optional<Conversation> findDirectConversation(UserId userA, UserId userB) {
        try {
            return jpaRepository.findDirectConversation(userA.value(), userB.value())
                    .map(ConversationEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de la conversation directe", ex);
        }
    }

    @Override
    public List<Conversation> findByParticipant(UserId userId) {
        try {
            return jpaRepository.findByParticipant(userId.value()).stream()
                    .map(ConversationEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des conversations", ex);
        }
    }
}
