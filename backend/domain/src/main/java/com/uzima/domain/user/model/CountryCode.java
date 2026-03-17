package com.uzima.domain.user.model;

import com.uzima.domain.shared.DomainException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Value Object : Code pays ISO 3166-1 alpha-2 (ex: "CM", "FR", "US").
 * Utilisé pour :
 * - Afficher le drapeau et l'indicatif international lors de l'inscription
 * - Valider la cohérence entre le code pays et le numéro de téléphone
 * Invariants :
 * - Non nul, non vide
 * - Exactement 2 caractères alphabétiques majuscules
 * - Code reconnu par java.util.Locale (validation statique minimale)
 * La validation approfondie (cohérence avec le numéro) est effectuée
 * par PhoneValidationPort (application) via libphonenumber.
 * Immuable par record.
 */
public record CountryCode(String value) {

    private static final Set<String> VALID_ISO_CODES = Set.of(Locale.getISOCountries());

    public CountryCode {
        Objects.requireNonNull(value, "Le code pays ne peut pas être nul");
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() != 2 || !normalized.chars().allMatch(Character::isLetter)) {
            throw new InvalidCountryCodeException(
                "Code pays invalide : '" + value + "'. Format attendu : ISO 3166-1 alpha-2 (ex: CM, FR, US)"
            );
        }
        if (!VALID_ISO_CODES.contains(normalized)) {
            throw new InvalidCountryCodeException(
                "Code pays inconnu : '" + normalized + "'"
            );
        }
        value = normalized;
    }

    public static CountryCode of(String value) {
        return new CountryCode(value);
    }

    /** Retourne le nom du pays dans la locale par défaut (utile pour les logs). */
    public String displayName() {
        return new Locale.Builder().setRegion(value).build().getDisplayCountry();
    }

    /** Retourne le préfixe téléphonique international estimé (heuristique — la validation précise est déléguée à libphonenumber). */
    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidCountryCodeException extends DomainException {
        public InvalidCountryCodeException(String message) {
            super(message);
        }
    }
}
