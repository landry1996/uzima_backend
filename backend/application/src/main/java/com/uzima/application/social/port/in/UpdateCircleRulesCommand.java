package com.uzima.application.social.port.in;

import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.social.model.CircleRule;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Mise à jour des règles d'un Cercle de Vie.
 * Réservée aux ADMIN et OWNER (vérifié par Circle.updateRules()).
 */
public record UpdateCircleRulesCommand(
        CircleId   circleId,
        UserId     requesterId,
        CircleRule newRules
) {
    public UpdateCircleRulesCommand {
        Objects.requireNonNull(circleId,    "L'identifiant du cercle est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(newRules,    "Les nouvelles règles sont obligatoires");
    }
}
