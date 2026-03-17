package com.uzima.application.user.port.out;

/**
 * Port de sortie : Validation approfondie du numéro de téléphone selon le pays.
 * <p>
 * La validation de base (format E.164) est effectuée par le VO PhoneNumber dans le domaine.
 * Ce port délègue la validation avancée à libphonenumber (infrastructure) pour vérifier :
 * - La cohérence entre le code pays et l'indicatif international
 * - La validité du numéro selon les règles de l'opérateur/pays
 * - La normalisation complète
 * <p>
 * Appartient à la couche application (pas au domaine) car c'est une dépendance sur
 * une bibliothèque externe (préoccupation technique).
 * Implémenté dans la couche infrastructure par LibPhoneNumberAdapter.
 */
public interface PhoneValidationPort {

    /**
     * Valide qu'un numéro de téléphone est cohérent avec le code pays donné.
     *
     * @param rawPhoneNumber Numéro de téléphone (format E.164 ou local)
     * @param countryCode    Code pays ISO 3166-1 alpha-2 (ex: "CM", "FR")
     * @return Résultat de la validation avec le numéro normalisé en E.164
     * @throws PhoneValidationException si le numéro est invalide pour ce pays
     */
    ValidationResult validate(String rawPhoneNumber, String countryCode);

    record ValidationResult(String normalizedE164, String countryCode, boolean isValid) {

        public static ValidationResult valid(String normalizedE164, String countryCode) {
            return new ValidationResult(normalizedE164, countryCode, true);
        }
    }

    final class PhoneValidationException extends RuntimeException {
        public PhoneValidationException(String message) {
            super(message);
        }
    }
}
