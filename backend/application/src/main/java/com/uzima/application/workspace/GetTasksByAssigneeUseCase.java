package com.uzima.application.workspace;

import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.Task;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Récupérer toutes les tâches assignées à un utilisateur, tous projets confondus.
 * Utile pour afficher la vue "Mes tâches" dans le dashboard personnel.
 */
public class GetTasksByAssigneeUseCase {

    private final TaskRepositoryPort taskRepository;

    public GetTasksByAssigneeUseCase(TaskRepositoryPort taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "Le repository de tâches est obligatoire");
    }

    public List<Task> execute(UserId assigneeId) {
        Objects.requireNonNull(assigneeId, "assigneeId est obligatoire");
        return taskRepository.findByAssigneeId(assigneeId);
    }
}
