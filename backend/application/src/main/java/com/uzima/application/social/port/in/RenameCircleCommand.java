package com.uzima.application.social.port.in;

import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Renommage d'un Cercle de Vie.
 * Réservée aux ADMIN et OWNER (vérifié par Circle.rename()).
 */
public record RenameCircleCommand(
        CircleId circleId,
        UserId   requesterId,
        String   newName
) {
    public RenameCircleCommand {
        Objects.requireNonNull(circleId,    "L'identifiant du cercle est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(newName,     "Le nouveau nom est obligatoire");
    }
}
