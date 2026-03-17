package com.uzima.domain.security;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object : Tentative de connexion.
 * Enregistre chaque tentative (succès ou échec) pour permettre
 * à AccountLockoutPolicy d'évaluer le verrouillage progressif.
 * Immuable par record. Appartient au domaine (aucune dépendance externe).
 * Note sécurité : l'identifiant est le numéro de téléphone normalisé.
 * Ne jamais stocker le mot de passe, même haché, dans cet objet.
 */
public record LoginAttempt(
        String identifier,    // numéro de téléphone normalisé (opaque)
        Instant attemptedAt,
        boolean successful
) {
    public LoginAttempt {
        Objects.requireNonNull(identifier, "L'identifiant est obligatoire");
        if (identifier.isBlank()) throw new IllegalArgumentException("L'identifiant ne peut pas être vide");
        Objects.requireNonNull(attemptedAt, "L'instant de tentative est obligatoire");
    }

    public boolean isFailed() {
        return !successful;
    }
}
