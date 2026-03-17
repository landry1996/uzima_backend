package com.uzima.application.user.port.in;

import java.util.Objects;

/**
 * Commande d'entrée : Authentification d'un utilisateur.
 */
public record AuthenticateUserCommand(
        String phoneNumber,
        String rawPassword
) {
    public AuthenticateUserCommand {
        Objects.requireNonNull(phoneNumber, "Le numéro de téléphone est obligatoire");
        Objects.requireNonNull(rawPassword, "Le mot de passe est obligatoire");
    }
}
