package com.uzima.domain.user.model;

import com.uzima.domain.shared.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object : Numéro de téléphone international.
 * Invariants :
 * - Format E.164 international (ex: +33612345678, +237612345678)
 * - Non nul, non vide
 * - 8 à 15 chiffres après le préfixe +
 * Immuable par conception (record).
 */
public record PhoneNumber(String value) {

    /**
     * Regex E.164 : commence par +, suivi de 8 à 15 chiffres.
     * Couvre les numéros africains, européens, etc.
     */
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "Le numéro de téléphone ne peut pas être nul");
        String normalized = value.strip().replaceAll("\\s+", "");
        if (!E164_PATTERN.matcher(normalized).matches()) {
            throw new InvalidPhoneNumberException(
                "Numéro de téléphone invalide : '" + value + "'. Format attendu : E.164 (ex: +33612345678)"
            );
        }
        value = normalized;
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidPhoneNumberException extends DomainException {
        public InvalidPhoneNumberException(String message) {
            super(message);
        }
    }
}
