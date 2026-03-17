package com.uzima.application.workspace.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;

import java.util.Objects;

/** Commande : Démarrage d'une entrée de temps. */
public record StartTimeEntryCommand(
        ProjectId projectId,
        UserId    userId,
        String    description  // optionnel
) {
    public StartTimeEntryCommand {
        Objects.requireNonNull(projectId, "L'identifiant du projet est obligatoire");
        Objects.requireNonNull(userId,    "L'identifiant utilisateur est obligatoire");
    }
}
