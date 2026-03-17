package com.uzima.application.workspace.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.TaskPriority;

import java.util.Objects;

/** Commande : Création d'une tâche dans un projet. */
public record CreateTaskCommand(
        String      title,
        ProjectId   projectId,
        UserId      requesterId,
        UserId      assigneeId,
        TaskPriority priority
) {
    public CreateTaskCommand {
        Objects.requireNonNull(title,       "Le titre est obligatoire");
        Objects.requireNonNull(projectId,   "L'identifiant du projet est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(priority,    "La priorité est obligatoire");
        // assigneeId est optionnel
    }
}
