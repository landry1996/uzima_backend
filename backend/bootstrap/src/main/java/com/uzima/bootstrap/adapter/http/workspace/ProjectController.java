package com.uzima.bootstrap.adapter.http.workspace;

import com.uzima.application.workspace.CreateProjectUseCase;
import com.uzima.application.workspace.CreateTaskUseCase;
import com.uzima.application.workspace.GetKanbanBoardUseCase;
import com.uzima.application.workspace.GetProjectsByMemberUseCase;
import com.uzima.application.workspace.GetRunningTimeEntryUseCase;
import com.uzima.application.workspace.GetTasksByAssigneeUseCase;
import com.uzima.application.workspace.GetTasksByStatusUseCase;
import com.uzima.application.workspace.TrackTimeUseCase;
import com.uzima.application.workspace.GetTimeReportUseCase;
import com.uzima.application.workspace.UpdateTaskStatusUseCase;
import com.uzima.application.workspace.port.in.CreateProjectCommand;
import com.uzima.application.workspace.port.in.CreateTaskCommand;
import com.uzima.application.workspace.port.in.StartTimeEntryCommand;
import com.uzima.application.workspace.port.in.StopTimeEntryCommand;
import com.uzima.application.workspace.port.in.UpdateTaskStatusCommand;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskId;
import com.uzima.domain.workspace.model.TaskStatus;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.workspace.model.TaskPriority;
import com.uzima.domain.workspace.model.TimeEntryId;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.user.model.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Adaptateur HTTP entrant : Workspace (Projets, Tâches, Time Tracking).
 * <p>
 * L'identité de l'utilisateur est extraite du JWT via {@link SecurityContextHelper}.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final CreateProjectUseCase      createProjectUseCase;
    private final CreateTaskUseCase         createTaskUseCase;
    private final UpdateTaskStatusUseCase   updateTaskStatusUseCase;
    private final GetKanbanBoardUseCase     getKanbanBoardUseCase;
    private final TrackTimeUseCase          trackTimeUseCase;
    private final GetTimeReportUseCase      getTimeReportUseCase;
    private final GetProjectsByMemberUseCase  getProjectsByMemberUseCase;
    private final GetTasksByStatusUseCase      getTasksByStatusUseCase;
    private final GetTasksByAssigneeUseCase    getTasksByAssigneeUseCase;
    private final GetRunningTimeEntryUseCase   getRunningTimeEntryUseCase;

    public ProjectController(
            CreateProjectUseCase       createProjectUseCase,
            CreateTaskUseCase          createTaskUseCase,
            UpdateTaskStatusUseCase    updateTaskStatusUseCase,
            GetKanbanBoardUseCase      getKanbanBoardUseCase,
            TrackTimeUseCase           trackTimeUseCase,
            GetTimeReportUseCase       getTimeReportUseCase,
            GetProjectsByMemberUseCase getProjectsByMemberUseCase,
            GetTasksByStatusUseCase    getTasksByStatusUseCase,
            GetTasksByAssigneeUseCase  getTasksByAssigneeUseCase,
            GetRunningTimeEntryUseCase getRunningTimeEntryUseCase
    ) {
        this.createProjectUseCase       = Objects.requireNonNull(createProjectUseCase);
        this.createTaskUseCase          = Objects.requireNonNull(createTaskUseCase);
        this.updateTaskStatusUseCase    = Objects.requireNonNull(updateTaskStatusUseCase);
        this.getKanbanBoardUseCase      = Objects.requireNonNull(getKanbanBoardUseCase);
        this.trackTimeUseCase           = Objects.requireNonNull(trackTimeUseCase);
        this.getTimeReportUseCase       = Objects.requireNonNull(getTimeReportUseCase);
        this.getProjectsByMemberUseCase = Objects.requireNonNull(getProjectsByMemberUseCase);
        this.getTasksByStatusUseCase    = Objects.requireNonNull(getTasksByStatusUseCase);
        this.getTasksByAssigneeUseCase  = Objects.requireNonNull(getTasksByAssigneeUseCase);
        this.getRunningTimeEntryUseCase = Objects.requireNonNull(getRunningTimeEntryUseCase);
    }

    /**
     * GET /api/projects?memberId=...
     * Retourne tous les projets dont l'utilisateur est membre (owner inclus).
     * 200 OK + liste de ProjectSummary
     */
    @GetMapping
    public ResponseEntity<List<ProjectSummary>> getProjectsByMember() {
        UserId userId = SecurityContextHelper.currentUserId();
        List<Project> projects = getProjectsByMemberUseCase.execute(userId);
        return ResponseEntity.ok(projects.stream().map(ProjectSummary::from).toList());
    }

    /**
     * POST /api/projects
     * Crée un nouveau projet.
     * 201 Created + projectId
     */
    @PostMapping
    public ResponseEntity<ProjectIdResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command   = new CreateProjectCommand(request.name(), requesterId);
        var projectId = createProjectUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectIdResponse(projectId.toString()));
    }

    /**
     * GET /api/projects/{id}/kanban
     * Retourne le tableau Kanban du projet.
     * 200 OK + KanbanResponse
     */
    @GetMapping("/{id}/kanban")
    public ResponseEntity<KanbanResponse> getKanban(
            @PathVariable String id
    ) {
        var view = getKanbanBoardUseCase.execute(ProjectId.of(id));
        return ResponseEntity.ok(KanbanResponse.from(view));
    }

    /**
     * GET /api/projects/{id}/tasks?status=TODO|IN_PROGRESS|...
     * Retourne les tâches d'un projet filtrées par statut.
     * 200 OK + liste de TaskResponse
     */
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {
        List<Task> tasks = getTasksByStatusUseCase.execute(
            ProjectId.of(id), TaskStatus.valueOf(status.toUpperCase())
        );
        return ResponseEntity.ok(tasks.stream().map(TaskResponse::from).toList());
    }

    /**
     * GET /api/projects/my-tasks
     * Retourne toutes les tâches assignées à l'utilisateur courant, tous projets confondus.
     * 200 OK + liste de TaskResponse
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks() {
        UserId userId = SecurityContextHelper.currentUserId();
        List<Task> tasks = getTasksByAssigneeUseCase.execute(userId);
        return ResponseEntity.ok(tasks.stream().map(TaskResponse::from).toList());
    }

    /**
     * POST /api/projects/{id}/tasks
     * Crée une tâche dans le projet (MANAGER+ requis).
     * 201 Created + taskId
     */
    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskIdResponse> createTask(
            @PathVariable String id,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = new CreateTaskCommand(
            request.title(),
            ProjectId.of(id),
            requesterId,
            request.assigneeId() != null ? UserId.of(request.assigneeId()) : null,
            TaskPriority.valueOf(request.priority().toUpperCase())
        );
        var taskId = createTaskUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TaskIdResponse(taskId.toString()));
    }

    /**
     * PATCH /api/projects/{id}/tasks/{taskId}/status
     * Met à jour le statut d'une tâche.
     * 204 No Content
     */
    @PatchMapping("/{id}/tasks/{taskId}/status")
    public ResponseEntity<Void> updateTaskStatus(
            @PathVariable String id,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = new UpdateTaskStatusCommand(
            TaskId.of(taskId),
            requesterId,
            UpdateTaskStatusCommand.Action.valueOf(request.action().toUpperCase()),
            request.reason()
        );
        updateTaskStatusUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/projects/time-entries/running
     * Retourne l'entrée de temps en cours de l'utilisateur, ou 204 si aucune.
     */
    @GetMapping("/time-entries/running")
    public ResponseEntity<TimeEntryResponse> getRunningTimeEntry() {
        UserId userId = SecurityContextHelper.currentUserId();
        return getRunningTimeEntryUseCase.execute(userId)
            .map(e -> ResponseEntity.ok(TimeEntryResponse.from(e)))
            .orElse(ResponseEntity.noContent().build());
    }

    /**
     * POST /api/projects/{id}/time-entries
     * Démarre une entrée de temps dans le projet.
     * 201 Created + timeEntryId
     */
    @PostMapping("/{id}/time-entries")
    public ResponseEntity<TimeEntryIdResponse> startTimeEntry(
            @PathVariable String id,
            @RequestBody(required = false) StartTimeEntryRequest request
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        var command = new StartTimeEntryCommand(
            ProjectId.of(id),
            userId,
            request != null ? request.description() : null
        );
        var timeEntryId = trackTimeUseCase.startEntry(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TimeEntryIdResponse(timeEntryId.toString()));
    }

    /**
     * PATCH /api/projects/{id}/time-entries/{entryId}/stop
     * Arrête une entrée de temps.
     * 204 No Content
     */
    @PatchMapping("/{id}/time-entries/{entryId}/stop")
    public ResponseEntity<Void> stopTimeEntry(
            @PathVariable String id,
            @PathVariable String entryId
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        var command = new StopTimeEntryCommand(TimeEntryId.of(entryId), userId);
        trackTimeUseCase.stopEntry(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/projects/{id}/time-report
     * Rapport de temps du projet.
     * 200 OK + TimeReportResponse
     */
    @GetMapping("/{id}/time-report")
    public ResponseEntity<TimeReportResponse> getTimeReport(@PathVariable String id) {
        var view = getTimeReportUseCase.forProject(ProjectId.of(id));
        return ResponseEntity.ok(TimeReportResponse.from(view));
    }

    // -------------------------------------------------------------------------
    // Request DTOs
    // -------------------------------------------------------------------------

    public record CreateProjectRequest(
            @NotBlank @Size(max = 150) String name
    ) {}

    public record CreateTaskRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull String priority,
            String assigneeId
    ) {}

    public record UpdateTaskStatusRequest(
            @NotBlank String action,
            String reason
    ) {}

    public record StartTimeEntryRequest(String description) {}

    // -------------------------------------------------------------------------
    // Response DTOs
    // -------------------------------------------------------------------------

    public record ProjectIdResponse(String projectId) {}

    public record ProjectSummary(
            String projectId,
            String name,
            String ownerId,
            int memberCount,
            int taskCount
    ) {
        public static ProjectSummary from(Project p) {
            return new ProjectSummary(
                p.id().toString(),
                p.name(),
                p.ownerId().toString(),
                p.memberCount(),
                p.taskCount()
            );
        }
    }
    public record TaskIdResponse(String taskId) {}

    public record TaskResponse(
            String taskId,
            String projectId,
            String title,
            String status,
            String priority,
            String assigneeId,
            boolean blocked,
            String blockedReason
    ) {
        public static TaskResponse from(Task t) {
            return new TaskResponse(
                t.id().toString(),
                t.projectId().toString(),
                t.title(),
                t.status().name(),
                t.priority().name(),
                t.assigneeId().map(UserId::toString).orElse(null),
                t.isBlocked(),
                t.blockedReason().orElse(null)
            );
        }
    }
    public record TimeEntryIdResponse(String timeEntryId) {}

    public record TimeEntryResponse(
            String timeEntryId,
            String projectId,
            String userId,
            Instant startedAt,
            Instant stoppedAt,
            Long durationMinutes,
            String description,
            boolean running
    ) {
        public static TimeEntryResponse from(TimeEntry e) {
            return new TimeEntryResponse(
                e.id().toString(),
                e.projectId().toString(),
                e.userId().toString(),
                e.startedAt(),
                e.stoppedAt().orElse(null),
                e.duration().map(Duration::toMinutes).orElse(null),
                e.description().orElse(null),
                e.isRunning()
            );
        }
    }

    public record KanbanResponse(
            String projectId,
            String projectName,
            int totalTasks,
            List<TaskSummary> backlog,
            List<TaskSummary> todo,
            List<TaskSummary> inProgress,
            List<TaskSummary> inReview,
            List<TaskSummary> done
    ) {
        public static KanbanResponse from(GetKanbanBoardUseCase.KanbanBoardView view) {
            return new KanbanResponse(
                view.project().id().toString(),
                view.project().name(),
                view.totalTasks(),
                view.column(com.uzima.domain.workspace.model.TaskStatus.BACKLOG).stream()
                        .map(TaskSummary::from).toList(),
                view.column(com.uzima.domain.workspace.model.TaskStatus.TODO).stream()
                        .map(TaskSummary::from).toList(),
                view.column(com.uzima.domain.workspace.model.TaskStatus.IN_PROGRESS).stream()
                        .map(TaskSummary::from).toList(),
                view.column(com.uzima.domain.workspace.model.TaskStatus.IN_REVIEW).stream()
                        .map(TaskSummary::from).toList(),
                view.column(com.uzima.domain.workspace.model.TaskStatus.DONE).stream()
                        .map(TaskSummary::from).toList()
            );
        }
    }

    public record TaskSummary(String taskId, String title, String priority, boolean blocked) {
        public static TaskSummary from(com.uzima.domain.workspace.model.Task task) {
            return new TaskSummary(
                task.id().toString(),
                task.title(),
                task.priority().displayName(),
                task.isBlocked()
            );
        }
    }

    public record TimeReportResponse(
            long totalMinutes,
            long stoppedCount,
            long runningCount
    ) {
        public static TimeReportResponse from(GetTimeReportUseCase.TimeReportView view) {
            Duration total = view.totalDuration();
            return new TimeReportResponse(
                total.toMinutes(),
                view.stoppedCount(),
                view.runningCount()
            );
        }
    }
}
