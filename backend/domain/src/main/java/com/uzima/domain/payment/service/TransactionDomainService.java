package com.uzima.domain.payment.service;

import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.payment.port.TransactionRepository;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service de domaine : opérations transversales sur les transactions.
 * Responsabilités :
 * - Récupération d'une transaction avec vérification d'existence
 * - Historique paginé (envoyé + reçu) pour un utilisateur
 * - Comptage total (pour pagination)
 * N'orchestre PAS les gateways externes (rôle du Use Case).
 * Utilise uniquement le port de domaine TransactionRepository.
 */
public final class TransactionDomainService {

    private final TransactionRepository transactionRepository;

    public TransactionDomainService(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(
            transactionRepository, "Le repository de transactions est obligatoire"
        );
    }

    /**
     * Retourne les transactions envoyées par un utilisateur, paginées.
     *
     * @param senderId Identifiant de l'expéditeur
     * @param limit    Nombre maximum de résultats (> 0)
     * @param offset   Décalage pour la pagination (>= 0)
     * @return Liste des transactions triées par date décroissante
     */
    public List<Transaction> getSentTransactions(UserId senderId, int limit, int offset) {
        Objects.requireNonNull(senderId, "L'identifiant de l'expéditeur est obligatoire");
        return transactionRepository.findBySenderId(senderId, limit, offset);
    }

    /**
     * Retourne les transactions reçues par un utilisateur, paginées.
     *
     * @param recipientId Identifiant du destinataire
     * @param limit       Nombre maximum de résultats (> 0)
     * @param offset      Décalage pour la pagination (>= 0)
     * @return Liste des transactions triées par date décroissante
     */
    public List<Transaction> getReceivedTransactions(UserId recipientId, int limit, int offset) {
        Objects.requireNonNull(recipientId, "L'identifiant du destinataire est obligatoire");
        return transactionRepository.findByRecipientId(recipientId, limit, offset);
    }

    /**
     * Retourne le nombre total de transactions envoyées par un utilisateur.
     * Utilisé pour calculer le nombre de pages dans la pagination.
     *
     * @param senderId Identifiant de l'expéditeur
     * @return Nombre total de transactions envoyées
     */
    public long countSentTransactions(UserId senderId) {
        Objects.requireNonNull(senderId, "L'identifiant de l'expéditeur est obligatoire");
        return transactionRepository.countBySenderId(senderId);
    }

    /**
     * Retourne le nombre total de transactions reçues par un utilisateur.
     * Utilisé pour calculer le nombre de pages dans la pagination.
     *
     * @param recipientId Identifiant du destinataire
     * @return Nombre total de transactions reçues
     */
    public long countReceivedTransactions(UserId recipientId) {
        Objects.requireNonNull(recipientId, "L'identifiant du destinataire est obligatoire");
        return transactionRepository.countByRecipientId(recipientId);
    }

    /**
     * Recherche une transaction par son identifiant.
     *
     * @param id Identifiant de la transaction
     * @return La transaction si trouvée
     * @throws TransactionNotFoundException si aucune transaction ne correspond
     */
    public Transaction getById(TransactionId id) {
        Objects.requireNonNull(id, "L'identifiant de la transaction est obligatoire");
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    // -------------------------------------------------------------------------
    // Exception domaine
    // -------------------------------------------------------------------------

    public static final class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(TransactionId id) {
            super("Transaction introuvable : " + id.value());
        }
    }
}
