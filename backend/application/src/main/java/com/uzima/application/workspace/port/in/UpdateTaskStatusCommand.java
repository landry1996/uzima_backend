package com.uzima.application.workspace.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.TaskId;

import java.util.Objects;

/** Commande : Mise à jour du statut d'une tâche. */
public record UpdateTaskStatusCommand(
        TaskId   taskId,
        UserId   requesterId,
        Action   action,
        String   reason      // utilisé uniquement pour Action.BLOCK
) {
    public UpdateTaskStatusCommand {
        Objects.requireNonNull(taskId,      "L'identifiant de la tâche est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(action,      "L'action est obligatoire");
    }

    /** Actions possibles sur le statut d'une tâche. */
    public enum Action {
        START,
        COMPLETE,
        SUBMIT_FOR_REVIEW,
        BLOCK,
        REOPEN
    }
}
