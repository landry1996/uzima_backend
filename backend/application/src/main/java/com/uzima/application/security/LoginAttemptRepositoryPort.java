package com.uzima.application.security;

import com.uzima.domain.security.LoginAttempt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Port de sortie : Stockage et récupération des tentatives de connexion.
 * Appartient à l'application (pas au domaine) car la persistance
 * est une préoccupation technique.
 * Implémenté par l'infrastructure (InMemory en dev, Redis ou JPA en prod).
 */
public interface LoginAttemptRepositoryPort {

    /**
     * Enregistre une tentative de connexion.
     */
    void save(LoginAttempt attempt);

    /**
     * Récupère les tentatives récentes pour un identifiant donné.
     *
     * @param identifier L'identifiant (numéro de téléphone normalisé)
     * @param within     Fenêtre temporelle (ex: Duration.ofHours(24))
     * @param from       Instant de référence (fourni par TimeProvider)
     * @return Liste des tentatives dans la fenêtre, triées par date décroissante
     */
    List<LoginAttempt> findRecentAttempts(String identifier, Duration within, Instant from);

    /**
     * Supprime toutes les tentatives pour un identifiant (après succès ou expiration).
     * Optionnel : peut être géré par TTL en Redis.
     */
    void clearAttempts(String identifier);
}
