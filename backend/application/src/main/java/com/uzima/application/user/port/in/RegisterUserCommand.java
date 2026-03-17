package com.uzima.application.user.port.in;

import java.util.Objects;

/**
 * Commande d'entrée : Inscription d'un nouvel utilisateur.
 * <p>
 * Objet immuable transportant les données brutes nécessaires à l'use case.
 * La validation métier (format téléphone, code pays, etc.) est déléguée
 * aux Value Objects du domaine et au PhoneValidationPort.
 * <p>
 * Immuable par record.
 */
public record RegisterUserCommand(
        String phoneNumber,
        String countryCode,
        String firstName,
        String lastName,
        String rawPassword
) {
    public RegisterUserCommand {
        Objects.requireNonNull(phoneNumber, "Le numéro de téléphone est obligatoire");
        Objects.requireNonNull(countryCode, "Le code pays est obligatoire");
        Objects.requireNonNull(firstName, "Le prénom est obligatoire");
        Objects.requireNonNull(lastName, "Le nom de famille est obligatoire");
        Objects.requireNonNull(rawPassword, "Le mot de passe est obligatoire");
        if (rawPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
        }
    }
}
