package com.uzima.application.workspace;

import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskStatus;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Récupérer les tâches d'un projet filtrées par statut.
 * Utile pour afficher une colonne spécifique du Kanban ou exporter les tâches DONE.
 */
public class GetTasksByStatusUseCase {

    private final TaskRepositoryPort taskRepository;

    public GetTasksByStatusUseCase(TaskRepositoryPort taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "Le repository de tâches est obligatoire");
    }

    public List<Task> execute(ProjectId projectId, TaskStatus status) {
        Objects.requireNonNull(projectId, "projectId est obligatoire");
        Objects.requireNonNull(status,    "status est obligatoire");
        return taskRepository.findByProjectIdAndStatus(projectId, status);
    }
}
