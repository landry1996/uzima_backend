package com.uzima.application.social.port.in;

import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.social.model.MemberRole;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Ajout d'un membre dans un Cercle de Vie.
 * <p>
 * Seul un ADMIN ou OWNER peut ajouter des membres.
 * Le rôle ne peut pas être OWNER (un cercle n'a qu'un seul OWNER).
 */
public record AddMemberCommand(
        CircleId   circleId,
        UserId     requesterId,
        UserId     newMemberId,
        MemberRole role
) {
    public AddMemberCommand {
        Objects.requireNonNull(circleId,    "L'identifiant du cercle est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(newMemberId, "L'identifiant du nouveau membre est obligatoire");
        Objects.requireNonNull(role,        "Le rôle est obligatoire");
    }
}
