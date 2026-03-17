package com.uzima.application.workspace;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.workspace.port.in.CreateTaskCommand;
import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskId;

import java.util.Objects;

/**
 * Use Case : Création d'une tâche et référencement dans un projet.
 * <p>
 * Orchestration :
 * 1. Charger le projet (404 si inexistant)
 * 2. Créer la tâche via Task.create()
 * 3. Référencer la tâche dans le projet (contrôle d'autorisation MANAGER+)
 * 4. Persister la tâche et le projet
 * 5. Retourner le TaskId
 */
public final class CreateTaskUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final TaskRepositoryPort    taskRepository;
    private final TimeProvider          clock;

    public CreateTaskUseCase(
            ProjectRepositoryPort projectRepository,
            TaskRepositoryPort taskRepository,
            TimeProvider clock
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "Le repository de projets est obligatoire");
        this.taskRepository    = Objects.requireNonNull(taskRepository,    "Le repository de tâches est obligatoire");
        this.clock             = Objects.requireNonNull(clock,             "Le fournisseur de temps est obligatoire");
    }

    /**
     * @return L'identifiant de la tâche créée
     * @throws ResourceNotFoundException                    si le projet est introuvable
     * @throws Task.InvalidTaskTitleException               si le titre est invalide
     * @throws Project.InsufficientProjectPermissionException si le demandeur n'est pas MANAGER+
     */
    public TaskId execute(CreateTaskCommand command) {
        Objects.requireNonNull(command, "La commande de création est obligatoire");

        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> ResourceNotFoundException.projectNotFound(command.projectId()));

        Task task = Task.create(
            command.title(),
            command.projectId(),
            command.assigneeId(),
            command.priority(),
            clock
        );

        project.addTask(task, command.requesterId());

        taskRepository.save(task);
        projectRepository.save(project);

        return task.id();
    }
}
