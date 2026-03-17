package com.uzima.application.workspace;

import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.Project;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Récupérer tous les projets auxquels un utilisateur appartient en tant que membre.
 * Inclut les projets dont il est owner ainsi que ceux où il a été ajouté comme membre.
 */
public class GetProjectsByMemberUseCase {

    private final ProjectRepositoryPort projectRepository;

    public GetProjectsByMemberUseCase(ProjectRepositoryPort projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "Le repository de projets est obligatoire");
    }

    public List<Project> execute(UserId userId) {
        Objects.requireNonNull(userId, "userId est obligatoire");
        return projectRepository.findByMemberId(userId);
    }
}
