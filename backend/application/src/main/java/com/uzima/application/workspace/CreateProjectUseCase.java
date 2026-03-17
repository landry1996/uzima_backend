package com.uzima.application.workspace;

import com.uzima.application.workspace.port.in.CreateProjectCommand;
import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectId;

import java.util.Objects;

/**
 * Use Case : Création d'un projet Workspace.
 * <p>
 * Orchestration :
 * 1. Créer le projet via Project.create() (créateur auto-ajouté en OWNER)
 * 2. Persister le projet
 * 3. Retourner le ProjectId
 */
public final class CreateProjectUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final TimeProvider          clock;

    public CreateProjectUseCase(ProjectRepositoryPort projectRepository, TimeProvider clock) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "Le repository de projets est obligatoire");
        this.clock             = Objects.requireNonNull(clock,             "Le fournisseur de temps est obligatoire");
    }

    /**
     * @return L'identifiant du projet créé
     * @throws Project.InvalidProjectNameException si le nom est invalide
     */
    public ProjectId execute(CreateProjectCommand command) {
        Objects.requireNonNull(command, "La commande de création est obligatoire");

        Project project = Project.create(command.name(), command.requesterId(), clock);
        projectRepository.save(project);
        return project.id();
    }
}
