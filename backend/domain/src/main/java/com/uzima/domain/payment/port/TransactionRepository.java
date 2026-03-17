package com.uzima.domain.payment.port;

import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port OUT (sortie) : Persistance des transactions.
 *
 * Défini dans le domaine, implémenté dans l'infrastructure.
 * Aucune dépendance JPA/Spring dans cette interface.
 */
public interface TransactionRepository {

    /** Sauvegarde une transaction (création ou mise à jour). */
    void save(Transaction transaction);

    /** Recherche par identifiant technique. */
    Optional<Transaction> findById(TransactionId id);

    /**
     * Retourne les transactions envoyées par un utilisateur, triées par date décroissante.
     *
     * @param senderId Identifiant de l'expéditeur
     * @param limit    Nombre maximum de résultats
     * @param offset   Décalage pour la pagination
     */
    List<Transaction> findBySenderId(UserId senderId, int limit, int offset);

    /**
     * Retourne les transactions reçues par un utilisateur, triées par date décroissante.
     */
    List<Transaction> findByRecipientId(UserId recipientId, int limit, int offset);

    /** Compte total des transactions envoyées (pour pagination). */
    long countBySenderId(UserId senderId);

    /** Compte total des transactions reçues (pour pagination). */
    long countByRecipientId(UserId recipientId);
}
