package com.uzima.infrastructure.persistence.message;

import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente MessageRepositoryPort avec JPA.
 * Délègue le mapping à MessageEntityMapper.
 * Enveloppe les exceptions JPA en DatabaseException.
 */
public final class MessageRepositoryAdapter implements MessageRepositoryPort {

    private final SpringDataMessageRepository jpaRepository;

    public MessageRepositoryAdapter(SpringDataMessageRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(Message message) {
        try {
            jpaRepository.save(MessageEntityMapper.toJpaEntity(message));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde du message", ex);
        }
    }

    @Override
    public Optional<Message> findById(MessageId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(MessageEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche du message", ex);
        }
    }

    @Override
    public List<Message> findByConversationId(ConversationId conversationId, int limit, int offset) {
        try {
            int page = offset / Math.max(limit, 1);
            return jpaRepository
                    .findByConversationIdOrderBySentAtDesc(
                            conversationId.value(), PageRequest.of(page, limit)
                    )
                    .stream()
                    .map(MessageEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des messages", ex);
        }
    }

    @Override
    public long countByConversationId(ConversationId conversationId) {
        try {
            return jpaRepository.countByConversationIdAndDeletedFalse(conversationId.value());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors du comptage des messages", ex);
        }
    }

    @Override
    public List<Message> findByDetectedIntent(ConversationId conversationId, String intent) {
        try {
            return jpaRepository
                .findByConversationIdAndMetadataIntentAndDeletedFalse(conversationId.value(), intent)
                .stream()
                .map(MessageEntityMapper::toDomain)
                .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche par intention", ex);
        }
    }
}
