package com.uzima.domain.user.model;

import com.uzima.domain.shared.DomainException;

import java.util.Objects;

/**
 * Value Object : Prénom de l'utilisateur.
 * Invariants :
 * - Non nul, non vide (après trim)
 * - Entre 1 et 50 caractères
 * Immuable par record.
 */
public record FirstName(String value) {

    private static final int MAX_LENGTH = 50;

    public FirstName {
        Objects.requireNonNull(value, "Le prénom ne peut pas être nul");
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidFirstNameException("Le prénom ne peut pas être vide");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidFirstNameException(
                "Le prénom ne peut pas dépasser " + MAX_LENGTH + " caractères"
            );
        }
        value = trimmed;
    }

    public static FirstName of(String value) {
        return new FirstName(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidFirstNameException extends DomainException {
        public InvalidFirstNameException(String message) {
            super(message);
        }
    }
}
