package com.uzima.application.workspace.port.out;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskId;
import com.uzima.domain.workspace.model.TaskStatus;

import java.util.List;
import java.util.Optional;

/** Port OUT : Persistance des tâches. */
public interface TaskRepositoryPort {

    void save(Task task);

    Optional<Task> findById(TaskId id);

    List<Task> findByProjectId(ProjectId projectId);

    List<Task> findByProjectIdAndStatus(ProjectId projectId, TaskStatus status);

    List<Task> findByAssigneeId(UserId assigneeId);
}
