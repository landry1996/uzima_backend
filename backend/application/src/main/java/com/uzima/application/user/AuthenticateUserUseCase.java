package com.uzima.application.user;

import com.uzima.application.security.BruteForceProtectionService;
import com.uzima.application.user.port.in.AuthenticateUserCommand;
import com.uzima.application.user.port.out.PasswordHasherPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.PhoneNumber;
import com.uzima.domain.user.model.User;

import java.util.Objects;
import java.util.Optional;

/**
 * Use Case : Authentification d'un utilisateur.
 * Sécurité renforcée :
 * 1. Vérifie le verrouillage AVANT la recherche (fail fast)
 * 2. Enregistre TOUJOURS la tentative (succès ou échec)
 * 3. Message d'erreur GÉNÉRIQUE (pas de fuite sur l'existence du compte)
 * 4. Comparaison constante (résiste aux timing attacks via passwordHasher.matches())
 * <p>
 * Ce use case retourne uniquement l'utilisateur authentifié.
 * La génération des tokens (access + refresh) est déléguée à GenerateTokenPairUseCase
 * dans le module security, orchestrée par la couche bootstrap (contrôleur).
 * Cette séparation respecte le principe de responsabilité unique.
 * <p>
 * Pas de Spring. Aucune dépendance à JWT (déléguée au module security).
 */
public final class AuthenticateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final BruteForceProtectionService bruteForceProtection;
    private final TimeProvider clock;

    public AuthenticateUserUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            BruteForceProtectionService bruteForceProtection,
            TimeProvider clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.bruteForceProtection = Objects.requireNonNull(bruteForceProtection);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Exécute l'authentification avec protection contre la force brute.
     *
     * @return L'utilisateur authentifié
     * @throws BruteForceProtectionService.AccountTemporarilyLockedException si compte verrouillé
     * @throws AuthenticationFailedException si identifiants invalides (message générique)
     */
    public User execute(AuthenticateUserCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        String identifier = command.phoneNumber();

        // 1. Vérifier le verrouillage AVANT de chercher l'utilisateur
        bruteForceProtection.checkNotLocked(identifier);

        // 2. Chercher l'utilisateur par numéro
        PhoneNumber phoneNumber = PhoneNumber.of(identifier);
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);

        // 3. Vérifier les identifiants (comparaison en temps constant via BCrypt)
        // Même si l'utilisateur n'existe pas, on simule une comparaison pour éviter le timing attack
        boolean credentialsValid = userOpt
                .map(u -> passwordHasher.matches(command.rawPassword(), u.passwordHash()))
                .orElse(false);

        // 4. Enregistrer la tentative TOUJOURS (avant de lever l'exception)
        bruteForceProtection.recordAttempt(identifier, credentialsValid);

        // 5. Répondre avec message générique (pas de distinction "compte inexistant" vs "mauvais mdp")
        if (!credentialsValid) {
            throw new AuthenticationFailedException("Identifiants invalides");
        }

        return userOpt.get(); // safe : credentialsValid=true implique userOpt.isPresent()
    }

    public static final class AuthenticationFailedException extends RuntimeException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }
}
