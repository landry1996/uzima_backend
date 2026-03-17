package com.uzima.application.social.port.in;

import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Création d'un Cercle de Vie.
 * <p>
 * Les règles par défaut sont déduites du type dans Circle.create().
 * Le demandeur devient automatiquement OWNER.
 */
public record CreateCircleCommand(
        UserId     requesterId,
        String     name,
        CircleType type
) {
    public CreateCircleCommand {
        Objects.requireNonNull(requesterId, "L'identifiant du créateur est obligatoire");
        Objects.requireNonNull(name,        "Le nom du cercle est obligatoire");
        Objects.requireNonNull(type,        "Le type de cercle est obligatoire");
    }
}
