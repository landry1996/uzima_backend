package com.uzima.application.social.port.in;

import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Retrait d'un membre d'un Cercle de Vie.
 * <p>
 * Cas autorisés :
 * - ADMIN / OWNER retire n'importe quel membre (sauf l'OWNER)
 * - Un MEMBER peut quitter lui-même le cercle (requesterId == targetMemberId)
 */
public record RemoveMemberCommand(
        CircleId circleId,
        UserId   requesterId,
        UserId   targetMemberId
) {
    public RemoveMemberCommand {
        Objects.requireNonNull(circleId,       "L'identifiant du cercle est obligatoire");
        Objects.requireNonNull(requesterId,    "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(targetMemberId, "L'identifiant du membre à retirer est obligatoire");
    }

    /** Retourne true si le demandeur quitte lui-même le cercle. */
    public boolean isSelfRemoval() {
        return requesterId.equals(targetMemberId);
    }
}
