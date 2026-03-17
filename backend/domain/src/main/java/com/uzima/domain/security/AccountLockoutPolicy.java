package com.uzima.domain.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Service de domaine : Politique de verrouillage progressif des comptes.
 * Règles métier (verrouillage progressif) :
 *  5 échecs en 15 min → verrouillage 5 minutes
 * 10 échecs en 1 heure → verrouillage 30 minutes
 * 15 échecs en 24h → verrouillage 24 heures (verrouillage "dur")
 * Principes de sécurité respectés :
 * - Pas de fuite d'info : le message d'erreur est générique côté application
 * - Verrouillage progressif : pas de blocage immédiat (UX + sécurité)
 * - Fenêtres glissantes : comptage sur fenêtre temporelle, pas compteur absolu
 * - Réinitialisation automatique après un succès
 * Ce service est pur domaine : pas de framework, injectable via constructeur.
 */
public final class AccountLockoutPolicy {

    // Seuils configurables (valeurs par défaut en constantes)
    private final int softLockThreshold;       // 5 échecs → lock court
    private final int mediumLockThreshold;     // 10 échecs → lock moyen
    private final int hardLockThreshold;       // 15 échecs → lock long

    private final Duration softLockWindow;     // Fenêtre pour le seuil doux (15 min)
    private final Duration mediumLockWindow;   // Fenêtre pour le seuil moyen (1h)
    private final Duration hardLockWindow;     // Fenêtre pour le seuil dur (24h)

    private final Duration softLockDuration;   // 5 min
    private final Duration mediumLockDuration; // 30 min
    private final Duration hardLockDuration;   // 24h

    /**
     * Constructeur avec valeurs par défaut.
     */
    public AccountLockoutPolicy() {
        this(5, 10, 15,
             Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(24),
             Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(24));
    }

    /**
     * Constructeur personnalisable pour les tests ou configurations spécifiques.
     */
    public AccountLockoutPolicy(
            int softLockThreshold, int mediumLockThreshold, int hardLockThreshold,
            Duration softLockWindow, Duration mediumLockWindow, Duration hardLockWindow,
            Duration softLockDuration, Duration mediumLockDuration, Duration hardLockDuration
    ) {
        this.softLockThreshold = softLockThreshold;
        this.mediumLockThreshold = mediumLockThreshold;
        this.hardLockThreshold = hardLockThreshold;
        this.softLockWindow = softLockWindow;
        this.mediumLockWindow = mediumLockWindow;
        this.hardLockWindow = hardLockWindow;
        this.softLockDuration = softLockDuration;
        this.mediumLockDuration = mediumLockDuration;
        this.hardLockDuration = hardLockDuration;
    }

    /**
     * Évalue si un compte doit être verrouillé selon l'historique des tentatives.
     *
     * @param recentAttempts L'historique des tentatives (récupéré depuis le repository)
     * @param now            L'instant courant (fourni par TimeProvider, jamais Instant.now())
     * @return LockoutDecision : allowed ou locked avec durée restante
     */
    public LockoutDecision evaluate(List<LoginAttempt> recentAttempts, Instant now) {
        Objects.requireNonNull(recentAttempts, "L'historique des tentatives est obligatoire");
        Objects.requireNonNull(now, "L'instant courant est obligatoire");

        // Vérification du verrouillage dur (24h) - priorité maximale
        long failuresInHardWindow = countFailuresInWindow(recentAttempts, hardLockWindow, now);
        if (failuresInHardWindow >= hardLockThreshold) {
            return LockoutDecision.locked(hardLockDuration);
        }

        // Vérification du verrouillage moyen (1h)
        long failuresInMediumWindow = countFailuresInWindow(recentAttempts, mediumLockWindow, now);
        if (failuresInMediumWindow >= mediumLockThreshold) {
            return LockoutDecision.locked(mediumLockDuration);
        }

        // Vérification du verrouillage doux (15 min)
        long failuresInSoftWindow = countFailuresInWindow(recentAttempts, softLockWindow, now);
        if (failuresInSoftWindow >= softLockThreshold) {
            return LockoutDecision.locked(softLockDuration);
        }

        return LockoutDecision.allowed();
    }

    private long countFailuresInWindow(List<LoginAttempt> attempts, Duration window, Instant now) {
        Instant windowStart = now.minus(window);
        return attempts.stream()
                .filter(LoginAttempt::isFailed)
                .filter(a -> a.attemptedAt().isAfter(windowStart))
                .count();
    }
}
