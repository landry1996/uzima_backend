package com.uzima.domain.user.model;

import com.uzima.domain.shared.DomainException;

import java.util.Objects;

/**
 * Value Object : Nom de famille de l'utilisateur.
 * Invariants :
 * - Non nul, non vide (après trim)
 * - Entre 1 et 50 caractères
 * Immuable par record.
 */
public record LastName(String value) {

    private static final int MAX_LENGTH = 50;

    public LastName {
        Objects.requireNonNull(value, "Le nom de famille ne peut pas être nul");
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidLastNameException("Le nom de famille ne peut pas être vide");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidLastNameException(
                "Le nom de famille ne peut pas dépasser " + MAX_LENGTH + " caractères"
            );
        }
        value = trimmed;
    }

    public static LastName of(String value) {
        return new LastName(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidLastNameException extends DomainException {
        public InvalidLastNameException(String message) {
            super(message);
        }
    }
}
