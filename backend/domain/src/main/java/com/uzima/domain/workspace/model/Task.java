package com.uzima.domain.workspace.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate Root : Tâche Kanban d'un projet workspace.
 * <p>
 * Cycle de vie :
 *   BACKLOG → TODO → IN_PROGRESS → IN_REVIEW → DONE
 *   DONE → TODO (reopen)
 *   Toute tâche non-DONE peut être bloquée (champ blockedReason, statut inchangé)
 * <p>
 * Invariants :
 * - Titre obligatoire, max 255 caractères
 * - Seul l'assigné ou un MANAGER+ peut démarrer la tâche (vérifié par use case)
 * - Une tâche DONE ne peut pas être complétée à nouveau
 */
public final class Task {

    private final TaskId    id;
    private final String    title;
    private final ProjectId projectId;
    private       UserId    assigneeId;
    private       TaskStatus  status;
    private final TaskPriority priority;
    private final String    description;
    private       String    blockedReason;
    private final Instant   createdAt;
    private       Instant   updatedAt;
    private       Instant   completedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Task(
            TaskId id, String title, ProjectId projectId, UserId assigneeId,
            TaskStatus status, TaskPriority priority, String description,
            String blockedReason, Instant createdAt, Instant updatedAt, Instant completedAt
    ) {
        this.id           = Objects.requireNonNull(id,         "L'identifiant est obligatoire");
        this.projectId    = Objects.requireNonNull(projectId,  "L'identifiant de projet est obligatoire");
        this.status       = Objects.requireNonNull(status,     "Le statut est obligatoire");
        this.priority     = Objects.requireNonNull(priority,   "La priorité est obligatoire");
        this.createdAt    = Objects.requireNonNull(createdAt,  "La date de création est obligatoire");
        this.updatedAt    = Objects.requireNonNull(updatedAt,  "La date de mise à jour est obligatoire");
        this.title        = validateTitle(title);
        this.assigneeId   = assigneeId;
        this.description  = description;
        this.blockedReason = blockedReason;
        this.completedAt  = completedAt;
    }

    // -------------------------------------------------------------------------
    // Factory : create()
    // -------------------------------------------------------------------------

    /**
     * Crée une nouvelle tâche en état BACKLOG.
     *
     * @throws InvalidTaskTitleException si le titre est invalide
     */
    public static Task create(
            String title, ProjectId projectId, UserId assigneeId,
            TaskPriority priority, TimeProvider clock
    ) {
        Objects.requireNonNull(projectId, "L'identifiant de projet est obligatoire");
        Objects.requireNonNull(priority,  "La priorité est obligatoire");
        Objects.requireNonNull(clock,     "Le fournisseur de temps est obligatoire");

        Instant now = clock.now();
        Task task = new Task(
            TaskId.generate(), title, projectId, assigneeId,
            TaskStatus.BACKLOG, priority, null, null, now, now, null
        );
        task.domainEvents.add(new TaskStatusChangedEvent(task.id, null, TaskStatus.BACKLOG, now));
        return task;
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    public static Task reconstitute(
            TaskId id, String title, ProjectId projectId, UserId assigneeId,
            TaskStatus status, TaskPriority priority, String description,
            String blockedReason, Instant createdAt, Instant updatedAt, Instant completedAt
    ) {
        return new Task(
            id, title, projectId, assigneeId, status, priority,
            description, blockedReason, createdAt, updatedAt, completedAt
        );
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Démarre la tâche (BACKLOG/TODO → IN_PROGRESS).
     *
     * @throws IllegalTransitionException si la tâche n'est pas en BACKLOG ou TODO
     */
    public void start(UserId assigneeId, TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (this.status != TaskStatus.BACKLOG && this.status != TaskStatus.TODO) {
            throw new IllegalTransitionException(
                "Impossible de démarrer une tâche en état " + this.status.displayName()
            );
        }
        Instant now       = clock.now();
        TaskStatus previous = this.status;
        this.assigneeId   = assigneeId;
        this.status       = TaskStatus.IN_PROGRESS;
        this.blockedReason = null;
        this.updatedAt    = now;
        domainEvents.add(new TaskStatusChangedEvent(id, previous, TaskStatus.IN_PROGRESS, now));
    }

    /**
     * Marque la tâche comme terminée (IN_PROGRESS/IN_REVIEW → DONE).
     *
     * @throws IllegalTransitionException si la tâche n'est pas active
     */
    public void complete(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (!this.status.isActive()) {
            throw new IllegalTransitionException(
                "Impossible de terminer une tâche en état " + this.status.displayName()
                + (this.status.isTerminal() ? " [état terminal]" : "")
            );
        }
        Instant now       = clock.now();
        TaskStatus previous = this.status;
        this.status       = TaskStatus.DONE;
        this.completedAt  = now;
        this.blockedReason = null;
        this.updatedAt    = now;
        domainEvents.add(new TaskStatusChangedEvent(id, previous, TaskStatus.DONE, now));
    }

    /**
     * Bloque la tâche avec une raison (statut inchangé, blockedReason renseigné).
     * Applicable sur toute tâche non terminée.
     *
     * @throws IllegalTransitionException si la tâche est déjà DONE
     */
    public void block(String reason) {
        Objects.requireNonNull(reason, "La raison de blocage est obligatoire");
        if (reason.isBlank()) throw new InvalidBlockReasonException("La raison de blocage ne peut pas être vide");
        if (this.status.isTerminal()) {
            throw new IllegalTransitionException("Impossible de bloquer une tâche terminée");
        }
        this.blockedReason = reason;
    }

    /**
     * Réouvre une tâche terminée (DONE → TODO).
     *
     * @throws IllegalTransitionException si la tâche n'est pas DONE
     */
    public void reopen(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (this.status != TaskStatus.DONE) {
            throw new IllegalTransitionException(
                "Impossible de réouvrir une tâche en état " + this.status.displayName()
            );
        }
        Instant now = clock.now();
        this.status       = TaskStatus.TODO;
        this.completedAt  = null;
        this.blockedReason = null;
        this.updatedAt    = now;
        domainEvents.add(new TaskStatusChangedEvent(id, TaskStatus.DONE, TaskStatus.TODO, now));
    }

    /**
     * Soumet la tâche pour revue (IN_PROGRESS → IN_REVIEW).
     *
     * @throws IllegalTransitionException si la tâche n'est pas IN_PROGRESS
     */
    public void submitForReview(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (this.status != TaskStatus.IN_PROGRESS) {
            throw new IllegalTransitionException(
                "Impossible de soumettre en revue une tâche en état " + this.status.displayName()
            );
        }
        Instant now = clock.now();
        this.status    = TaskStatus.IN_REVIEW;
        this.updatedAt = now;
        domainEvents.add(new TaskStatusChangedEvent(id, TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW, now));
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isBlocked()   { return blockedReason != null; }
    public boolean isCompleted() { return status == TaskStatus.DONE; }
    public boolean isPending()   { return status == TaskStatus.BACKLOG || status == TaskStatus.TODO; }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> deduped = domainEvents.stream()
                .collect(Collectors.toMap(
                    DomainEvent::eventId, e -> e, (a, b) -> a, LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparing(DomainEvent::occurredAt))
                .toList();
        domainEvents.clear();
        return deduped;
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public TaskId       id()           { return id; }
    public String       title()        { return title; }
    public ProjectId    projectId()    { return projectId; }
    public TaskStatus   status()       { return status; }
    public TaskPriority priority()     { return priority; }
    public Instant      createdAt()    { return createdAt; }
    public Instant      updatedAt()    { return updatedAt; }

    public Optional<UserId>  assigneeId()    { return Optional.ofNullable(assigneeId); }
    public Optional<String>  description()   { return Optional.ofNullable(description); }
    public Optional<String>  blockedReason() { return Optional.ofNullable(blockedReason); }
    public Optional<Instant> completedAt()   { return Optional.ofNullable(completedAt); }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task t)) return false;
        return id.equals(t.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title='" + title + "', status=" + status.displayName()
             + ", priority=" + priority.displayName() + "}";
    }

    // -------------------------------------------------------------------------
    // Helper privé
    // -------------------------------------------------------------------------

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskTitleException("Le titre de la tâche ne peut pas être vide");
        }
        if (title.length() > 255) {
            throw new InvalidTaskTitleException("Le titre ne peut pas dépasser 255 caractères");
        }
        return title.strip();
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class InvalidTaskTitleException extends RuntimeException {
        public InvalidTaskTitleException(String message) { super(message); }
    }

    public static final class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(String message) { super(message); }
    }

    public static final class InvalidBlockReasonException extends RuntimeException {
        public InvalidBlockReasonException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Event
    // -------------------------------------------------------------------------

    public record TaskStatusChangedEvent(
            UUID       eventId,
            TaskId     taskId,
            TaskStatus previousStatus,
            TaskStatus newStatus,
            Instant    occurredAt
    ) implements DomainEvent {

        public TaskStatusChangedEvent(TaskId taskId, TaskStatus previousStatus, TaskStatus newStatus, Instant occurredAt) {
            this(UUID.randomUUID(), taskId, previousStatus, newStatus, occurredAt);
        }
    }
}
