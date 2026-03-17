package com.uzima.application.workspace.port.out;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectId;

import java.util.List;
import java.util.Optional;

/** Port OUT : Persistance des projets. */
public interface ProjectRepositoryPort {

    void save(Project project);

    Optional<Project> findById(ProjectId id);

    List<Project> findByOwnerId(UserId userId);

    List<Project> findByMemberId(UserId userId);
}
