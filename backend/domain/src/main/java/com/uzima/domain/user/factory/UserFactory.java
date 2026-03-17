package com.uzima.domain.user.factory;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.*;

import java.util.Objects;

/**
 * Factory métier : création d'un utilisateur.
 * <p>
 * Justification du pattern Factory :
 * - Encapsule la construction des Value Objects (PhoneNumber, CountryCode, FirstName, LastName)
 *   depuis des données brutes (String)
 * - La couche application n'a pas besoin d'importer les VOs individuellement
 * - Centralise le point de création, facilitant les évolutions
 * - Garantit les invariants par construction
 *
 * Différence avec User.register() :
 * - User.register() = factory method sur l'agrégat (DDD standard)
 * - UserFactory = factory de domaine qui orchestre la construction des VOs
 *   avant de déléguer à User.register()
 */
public final class UserFactory {

    private UserFactory() {
        // Classe utilitaire statique — ne pas instancier
    }

    /**
     * Crée un nouvel utilisateur à partir de données brutes.
     * <p>
     * Orchestre :
     * 1. Construction et validation de PhoneNumber (format E.164)
     * 2. Construction et validation de CountryCode (ISO 3166-1 alpha-2)
     * 3. Construction et validation de FirstName + LastName
     * 4. Délégation à User.register() (factory method de l'agrégat)
     *
     * @param rawPhoneNumber Numéro de téléphone brut en format E.164 (ex: "+237612345678")
     * @param rawCountryCode Code pays ISO 3166-1 alpha-2 (ex: "CM", "FR")
     * @param rawFirstName   Prénom brut
     * @param rawLastName    Nom de famille brut
     * @param passwordHash   Hash du mot de passe (fourni par PasswordHasherPort)
     * @param clock          Fournisseur de temps injecté
     * @return L'utilisateur créé, en état valide garanti
     * @throws PhoneNumber.InvalidPhoneNumberException   si le format E.164 est invalide
     * @throws CountryCode.InvalidCountryCodeException   si le code pays est invalide
     * @throws FirstName.InvalidFirstNameException       si le prénom est invalide
     * @throws LastName.InvalidLastNameException         si le nom est invalide
     * @throws User.InvalidPasswordHashException         si le hash est nul ou vide
     */
    public static User createUser(
            String rawPhoneNumber,
            String rawCountryCode,
            String rawFirstName,
            String rawLastName,
            String passwordHash,
            TimeProvider clock
    ) {
        Objects.requireNonNull(rawPhoneNumber, "Le numéro de téléphone est obligatoire");
        Objects.requireNonNull(rawCountryCode, "Le code pays est obligatoire");
        Objects.requireNonNull(rawFirstName, "Le prénom est obligatoire");
        Objects.requireNonNull(rawLastName, "Le nom de famille est obligatoire");
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        PhoneNumber phoneNumber = PhoneNumber.of(rawPhoneNumber);
        CountryCode country = CountryCode.of(rawCountryCode);
        FirstName firstName = FirstName.of(rawFirstName);
        LastName lastName = LastName.of(rawLastName);

        return User.register(phoneNumber, country, firstName, lastName, passwordHash, clock);
    }
}
