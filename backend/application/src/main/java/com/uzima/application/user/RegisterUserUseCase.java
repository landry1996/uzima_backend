package com.uzima.application.user;

import com.uzima.application.user.port.in.RegisterUserCommand;
import com.uzima.application.user.port.out.PasswordHasherPort;
import com.uzima.application.user.port.out.PhoneValidationPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.factory.UserFactory;
import com.uzima.domain.user.model.PhoneNumber;
import com.uzima.domain.user.model.User;

import java.util.Objects;

/**
 * Use Case : Inscription d'un nouvel utilisateur.
 * <p>
 * Flux :
 * 1. Valider la cohérence numéro/pays via PhoneValidationPort (libphonenumber)
 * 2. Vérifier l'unicité du numéro normalisé
 * 3. Hacher le mot de passe
 * 4. Créer via UserFactory (construit tous les VOs)
 * 5. Persister
 * <p>
 * Les exceptions typées sont gérées centralement par GlobalExceptionHandler.
 */
public final class RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final PhoneValidationPort phoneValidator;
    private final TimeProvider clock;

    public RegisterUserUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            PhoneValidationPort phoneValidator,
            TimeProvider clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.phoneValidator = Objects.requireNonNull(phoneValidator);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Exécute l'inscription.
     *
     * @param command Les données d'inscription (phoneNumber, countryCode, firstName, lastName, rawPassword)
     * @return L'utilisateur créé
     * @throws PhoneNumberAlreadyUsedException           si le numéro est déjà enregistré
     * @throws PhoneValidationPort.PhoneValidationException si le numéro est invalide pour le pays
     * @throws PhoneNumber.InvalidPhoneNumberException   si le format E.164 est invalide
     */
    public User execute(RegisterUserCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        // 1. Valider la cohérence numéro/pays (libphonenumber via port)
        //    Retourne le numéro normalisé en E.164 (ex: "0612345678" → "+33612345678")
        PhoneValidationPort.ValidationResult validation = phoneValidator.validate(
                command.phoneNumber(), command.countryCode()
        );

        String normalizedPhone = validation.normalizedE164();

        // 2. Vérifier l'unicité du numéro normalisé
        PhoneNumber phoneNumber = PhoneNumber.of(normalizedPhone);
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyUsedException(
                "Le numéro " + phoneNumber + " est déjà associé à un compte"
            );
        }

        // 3. Hacher le mot de passe (délégué à l'infrastructure)
        String passwordHash = passwordHasher.hash(command.rawPassword());

        // 4. Créer via UserFactory (délègue à User.register, construit les VOs)
        User user = UserFactory.createUser(
                normalizedPhone,
                command.countryCode(),
                command.firstName(),
                command.lastName(),
                passwordHash,
                clock
        );

        // 5. Persister
        userRepository.save(user);

        return user;
    }

    /**
     * Exception spécifique : conflit de numéro de téléphone.
     * Traduite en HTTP 409 par GlobalExceptionHandler.
     */
    public static final class PhoneNumberAlreadyUsedException extends RuntimeException {
        public PhoneNumberAlreadyUsedException(String message) {
            super(message);
        }
    }
}
