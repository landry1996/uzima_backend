package com.uzima.application.user.port.in;

import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Mise à jour de l'état de présence d'un utilisateur.
 */
public record UpdatePresenceStatusCommand(
        UserId userId,
        PresenceStatus newStatus
) {
    public UpdatePresenceStatusCommand {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(newStatus, "Le nouvel état de présence est obligatoire");
    }
}
