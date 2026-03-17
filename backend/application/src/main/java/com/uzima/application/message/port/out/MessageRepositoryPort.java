package com.uzima.application.message.port.out;

import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie (application) : persistance des messages.
 * Aligne le contrat avec le port domaine (MessageRepository)
 * en ajoutant countByConversationId pour le support de la pagination.
 */
public interface MessageRepositoryPort {

    void save(Message message);

    Optional<Message> findById(MessageId id);

    /**
     * Récupère les messages d'une conversation, du plus récent au plus ancien.
     *
     * @param conversationId Identifiant de la conversation
     * @param limit          Nombre maximum de messages à retourner
     * @param offset         Décalage (pour la pagination)
     */
    List<Message> findByConversationId(ConversationId conversationId, int limit, int offset);

    /**
     * Compte le nombre total de messages dans une conversation.
     * Utilisé pour calculer le nombre total de pages côté client.
     */
    long countByConversationId(ConversationId conversationId);

    /**
     * Recherche les messages d'une conversation dont l'intention IA détectée
     * correspond à la valeur donnée.
     */
    List<Message> findByDetectedIntent(ConversationId conversationId, String intent);
}
