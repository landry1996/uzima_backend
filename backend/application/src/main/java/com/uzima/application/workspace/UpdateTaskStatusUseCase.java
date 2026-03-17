package com.uzima.application.workspace;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.workspace.port.in.UpdateTaskStatusCommand;
import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.Task;

import java.util.Objects;

/**
 * Use Case : Mise à jour du statut d'une tâche.
 * <p>
 * Dispatche l'action vers la méthode domaine correspondante.
 * La logique de transition reste dans l'agrégat Task.
 */
public final class UpdateTaskStatusUseCase {

    private final TaskRepositoryPort taskRepository;
    private final TimeProvider       clock;

    public UpdateTaskStatusUseCase(TaskRepositoryPort taskRepository, TimeProvider clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "Le repository de tâches est obligatoire");
        this.clock          = Objects.requireNonNull(clock,          "Le fournisseur de temps est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException          si la tâche est introuvable
     * @throws Task.IllegalTransitionException    si la transition est invalide
     * @throws Task.InvalidBlockReasonException   si la raison de blocage est vide
     */
    public void execute(UpdateTaskStatusCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire");

        Task task = taskRepository.findById(command.taskId())
                .orElseThrow(() -> ResourceNotFoundException.taskNotFound(command.taskId()));

        switch (command.action()) {
            case START             -> task.start(command.requesterId(), clock);
            case COMPLETE          -> task.complete(clock);
            case SUBMIT_FOR_REVIEW -> task.submitForReview(clock);
            case BLOCK             -> task.block(command.reason());
            case REOPEN            -> task.reopen(clock);
        }

        taskRepository.save(task);
    }
}
