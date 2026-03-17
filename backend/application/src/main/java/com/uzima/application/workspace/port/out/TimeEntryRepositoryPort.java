package com.uzima.application.workspace.port.out;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.workspace.model.TimeEntryId;

import java.util.List;
import java.util.Optional;

/** Port OUT : Persistance des entrées de temps. */
public interface TimeEntryRepositoryPort {

    void save(TimeEntry timeEntry);

    Optional<TimeEntry> findById(TimeEntryId id);

    List<TimeEntry> findByProjectId(ProjectId projectId);

    List<TimeEntry> findByUserId(UserId userId);

    Optional<TimeEntry> findRunningForUser(UserId userId);
}
