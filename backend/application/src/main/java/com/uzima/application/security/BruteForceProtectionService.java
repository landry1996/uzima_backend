package com.uzima.application.security;

import com.uzima.domain.security.AccountLockoutPolicy;
import com.uzima.domain.security.LoginAttempt;
import com.uzima.domain.security.LockoutDecision;
import com.uzima.domain.shared.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Service applicatif : Protection contre la force brute.
 * Coordonne :
 * - AccountLockoutPolicy (domaine) : évalue les règles de verrouillage
 * - LoginAttemptRepositoryPort (infrastructure) : stocke/récupère l'historique
 * Utilisé par AuthenticateUserUseCase avant et après la vérification des identifiants.
 * Principe de sécurité fondamental :
 * - Le message d'erreur retourné est TOUJOURS générique ("Identifiants invalides")
 * - La durée de verrouillage peut être exposée au client pour améliorer l'UX
 * - Le verrouillage est appliqué par identifiant (téléphone) ET par IP (filtre HTTP)
 */
public final class BruteForceProtectionService {

    private final AccountLockoutPolicy lockoutPolicy;
    private final LoginAttemptRepositoryPort attemptRepository;
    private final TimeProvider clock;

    // Fenêtre de récupération de l'historique (doit couvrir la plus grande fenêtre de la policy)
    private static final Duration HISTORY_WINDOW = Duration.ofHours(25);

    public BruteForceProtectionService(
            AccountLockoutPolicy lockoutPolicy,
            LoginAttemptRepositoryPort attemptRepository,
            TimeProvider clock
    ) {
        this.lockoutPolicy = Objects.requireNonNull(lockoutPolicy);
        this.attemptRepository = Objects.requireNonNull(attemptRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Vérifie si un identifiant est actuellement verrouillé.
     * À appeler AVANT la vérification des identifiants.
     *
     * @param identifier Le numéro de téléphone
     * @throws AccountTemporarilyLockedException si le compte est verrouillé
     */
    public void checkNotLocked(String identifier) {
        Objects.requireNonNull(identifier);
        Instant now = clock.now();
        List<LoginAttempt> history = attemptRepository.findRecentAttempts(identifier, HISTORY_WINDOW, now);
        LockoutDecision decision = lockoutPolicy.evaluate(history, now);

        if (decision.isLocked()) {
            throw new AccountTemporarilyLockedException(
                identifier,
                decision.remainingLockDuration().orElse(Duration.ofMinutes(5))
            );
        }
    }

    /**
     * Enregistre le résultat d'une tentative.
     * À appeler APRÈS la vérification des identifiants (qu'elle réussisse ou non).
     *
     * @param identifier Le numéro de téléphone
     * @param successful true si l'authentification a réussi
     */
    public void recordAttempt(String identifier, boolean successful) {
        Objects.requireNonNull(identifier);
        LoginAttempt attempt = new LoginAttempt(identifier, clock.now(), successful);
        attemptRepository.save(attempt);

        // Après un succès : nettoyer l'historique (réinitialiser le compteur)
        if (successful) {
            attemptRepository.clearAttempts(identifier);
        }
    }

    /**
     * Exception levée quand un compte est temporairement verrouillé.
     * Traduite en HTTP 429 Too Many Requests par le GlobalExceptionHandler.
     */
    public static final class AccountTemporarilyLockedException extends RuntimeException {

        private final String identifier;
        private final Duration lockDuration;

        public AccountTemporarilyLockedException(String identifier, Duration lockDuration) {
            super("Compte temporairement verrouillé. Réessayez dans " + formatDuration(lockDuration));
            this.identifier = identifier;
            this.lockDuration = lockDuration;
        }

        public String identifier() { return identifier; }
        public Duration lockDuration() { return lockDuration; }

        private static String formatDuration(Duration d) {
            if (d.toHours() >= 1) return d.toHours() + " heure(s)";
            return d.toMinutes() + " minute(s)";
        }
    }
}
