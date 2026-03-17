package com.uzima.application.user.port.in;

import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Mise à jour du profil utilisateur.
 * Les champs null signifient "ne pas modifier".
 */
public record UpdateUserProfileCommand(
        UserId userId,
        String firstName,  // null = ne pas changer
        String lastName,   // null = ne pas changer
        String avatarUrl   // null = ne pas changer
) {
    public UpdateUserProfileCommand {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
    }
}
