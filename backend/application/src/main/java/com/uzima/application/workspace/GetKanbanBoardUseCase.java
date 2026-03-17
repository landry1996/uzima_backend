package com.uzima.application.workspace;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Use Case (Query) : Tableau Kanban d'un projet.
 * <p>
 * Retourne les tâches du projet regroupées par statut.
 */
public final class GetKanbanBoardUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final TaskRepositoryPort    taskRepository;

    public GetKanbanBoardUseCase(
            ProjectRepositoryPort projectRepository,
            TaskRepositoryPort taskRepository
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "Le repository de projets est obligatoire");
        this.taskRepository    = Objects.requireNonNull(taskRepository,    "Le repository de tâches est obligatoire");
    }

    /**
     * @param projectId Identifiant du projet
     * @return Vue Kanban avec les tâches groupées par statut
     * @throws ResourceNotFoundException si le projet est introuvable
     */
    public KanbanBoardView execute(ProjectId projectId) {
        Objects.requireNonNull(projectId, "L'identifiant du projet est obligatoire");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.projectNotFound(projectId));

        List<Task> tasks = taskRepository.findByProjectId(projectId);

        Map<TaskStatus, List<Task>> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, tasks.stream()
                    .filter(t -> t.status() == status)
                    .toList());
        }

        return new KanbanBoardView(project, byStatus, tasks.size());
    }

    // -------------------------------------------------------------------------
    // Vue de sortie
    // -------------------------------------------------------------------------

    /**
     * Vue Kanban d'un projet.
     *
     * @param project    Projet source
     * @param tasksByStatus Tâches regroupées par statut
     * @param totalTasks Nombre total de tâches
     */
    public record KanbanBoardView(
            Project              project,
            Map<TaskStatus, List<Task>> tasksByStatus,
            int                  totalTasks
    ) {
        public List<Task> column(TaskStatus status) {
            return tasksByStatus.getOrDefault(status, List.of());
        }
    }
}
