package com.uzima.bootstrap.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO HTTP entrant : Inscription.
 * Validation Bean Validation (Jakarta) uniquement ici — jamais dans le domaine.
 */
public record RegisterRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String phoneNumber,

        @NotBlank(message = "Le code pays est obligatoire")
        @Pattern(regexp = "[A-Z]{2}", message = "Le code pays doit être au format ISO 3166-1 alpha-2 (ex: CM, FR)")
        String countryCode,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 1, max = 50, message = "Le prénom doit contenir entre 1 et 50 caractères")
        String firstName,

        @NotBlank(message = "Le nom de famille est obligatoire")
        @Size(min = 1, max = 50, message = "Le nom de famille doit contenir entre 1 et 50 caractères")
        String lastName,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password
) {}
