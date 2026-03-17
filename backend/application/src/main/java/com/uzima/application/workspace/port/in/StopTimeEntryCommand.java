package com.uzima.application.workspace.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.TimeEntryId;

import java.util.Objects;

/** Commande : Arrêt d'une entrée de temps. */
public record StopTimeEntryCommand(TimeEntryId timeEntryId, UserId userId) {

    public StopTimeEntryCommand {
        Objects.requireNonNull(timeEntryId, "L'identifiant de l'entrée est obligatoire");
        Objects.requireNonNull(userId,      "L'identifiant utilisateur est obligatoire");
    }
}
