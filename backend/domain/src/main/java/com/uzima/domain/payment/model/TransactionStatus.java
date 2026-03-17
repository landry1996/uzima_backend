package com.uzima.domain.payment.model;

/**
 * Enum : États du cycle de vie d'une transaction.
 * Transitions autorisées :
 *   PENDING → COMPLETED (gateway confirme)
 *   PENDING → FAILED (gateway refuse ou erreur)
 *   PENDING → CANCELLED (annulée avant traitement)
 * États terminaux : COMPLETED, FAILED, CANCELLED (aucune transition possible)
 */
public enum TransactionStatus {

    /** Transaction créée, en attente de traitement par la gateway. */
    PENDING,

    /** Transaction traitée avec succès par la gateway. */
    COMPLETED,

    /** Transaction rejetée (fonds insuffisants, erreur gateway, fraude détectée). */
    FAILED,

    /** Transaction annulée par l'expéditeur avant traitement. */
    CANCELLED;

    /** Retourne true si la transaction est dans un état terminal (non modifiable). */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public String displayName() {
        return switch (this) {
            case PENDING   -> "En attente";
            case COMPLETED -> "Réussie";
            case FAILED    -> "Échouée";
            case CANCELLED -> "Annulée";
        };
    }
}
