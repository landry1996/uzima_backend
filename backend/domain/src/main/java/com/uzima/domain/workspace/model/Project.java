package com.uzima.domain.workspace.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate Root : Projet Workspace (Kanban conversationnel).
 * Agrège :
 * - Une liste de membres (ProjectMember) avec rôles
 * - Une liste de TaskId (les Task sont leurs propres Aggregates Roots)
 * - Une liste de TimeEntry (entités dans la frontière du projet)
 * Invariants :
 * - Nom obligatoire, max 150 caractères
 * - Seul un MANAGER+ peut ajouter des membres et des tâches
 * - Un utilisateur ne peut avoir qu'un seul rôle par projet
 */
public final class Project {

    private final ProjectId id;
    private final String    name;
    private final UserId    ownerId;
    private final Instant   createdAt;

    private final List<ProjectMember> members    = new ArrayList<>();
    private final List<TaskId>        taskIds    = new ArrayList<>();
    private final List<TimeEntry>     timeEntries = new ArrayList<>();
    private final List<DomainEvent>   domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Project(ProjectId id, String name, UserId ownerId, Instant createdAt) {
        this.id        = Objects.requireNonNull(id,      "L'identifiant est obligatoire");
        this.ownerId   = Objects.requireNonNull(ownerId, "Le propriétaire est obligatoire");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création est obligatoire");
        this.name      = validateName(name);
    }

    // -------------------------------------------------------------------------
    // Factory : create()
    // -------------------------------------------------------------------------

    /**
     * Crée un nouveau projet avec le créateur automatiquement ajouté en OWNER.
     *
     * @throws InvalidProjectNameException si le nom est invalide
     */
    public static Project create(String name, UserId ownerId, TimeProvider clock) {
        Objects.requireNonNull(ownerId, "Le propriétaire est obligatoire");
        Objects.requireNonNull(clock,   "Le fournisseur de temps est obligatoire");

        Instant now = clock.now();
        Project project = new Project(ProjectId.generate(), name, ownerId, now);
        project.members.add(new ProjectMember(ownerId, ProjectRole.OWNER, now));
        project.domainEvents.add(new ProjectCreatedEvent(project.id, ownerId, now));
        return project;
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    public static Project reconstitute(
            ProjectId id, String name, UserId ownerId, Instant createdAt,
            List<ProjectMember> members, List<TaskId> taskIds, List<TimeEntry> timeEntries
    ) {
        Project project = new Project(id, name, ownerId, createdAt);
        project.members.addAll(members);
        project.taskIds.addAll(taskIds);
        project.timeEntries.addAll(timeEntries);
        return project;
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Référence une tâche nouvellement créée dans le projet.
     * Le demandeur doit être MANAGER+.
     *
     * @throws InsufficientProjectPermissionException si le demandeur n'est pas MANAGER+
     * @throws DuplicateTaskException si la tâche est déjà référencée
     */
    public void addTask(Task task, UserId requesterId) {
        Objects.requireNonNull(task,        "La tâche est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");

        requireManagerPermission(requesterId);

        if (taskIds.contains(task.id())) {
            throw new DuplicateTaskException("La tâche " + task.id() + " est déjà dans le projet");
        }
        taskIds.add(task.id());
    }

    /**
     * Ajoute un membre au projet.
     * Le demandeur doit être MANAGER+.
     *
     * @throws InsufficientProjectPermissionException si le demandeur n'est pas MANAGER+
     * @throws DuplicateMemberException si l'utilisateur est déjà membre
     */
    public void addMember(UserId userId, ProjectRole role, UserId requesterId, TimeProvider clock) {
        Objects.requireNonNull(userId,      "L'identifiant du membre est obligatoire");
        Objects.requireNonNull(role,        "Le rôle est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(clock,       "Le fournisseur de temps est obligatoire");

        if (role == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Un projet ne peut avoir qu'un seul OWNER");
        }
        requireManagerPermission(requesterId);

        if (isMember(userId)) {
            throw new DuplicateMemberException("L'utilisateur " + userId + " est déjà membre du projet");
        }
        members.add(new ProjectMember(userId, role, clock.now()));
    }

    /**
     * Démarre une entrée de temps pour un membre du projet.
     *
     * @throws ProjectMembershipRequiredException si userId n'est pas membre
     * @throws ActiveTimeEntryExistsException si une entrée est déjà en cours pour cet utilisateur
     */
    public TimeEntry startTimeEntry(UserId userId, String description, TimeProvider clock) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");

        if (!isMember(userId)) {
            throw new ProjectMembershipRequiredException(
                "L'utilisateur " + userId + " n'est pas membre du projet " + id
            );
        }
        boolean hasRunning = timeEntries.stream()
                .anyMatch(e -> e.userId().equals(userId) && e.isRunning());
        if (hasRunning) {
            throw new ActiveTimeEntryExistsException(
                "L'utilisateur " + userId + " a déjà une entrée de temps en cours"
            );
        }
        TimeEntry entry = TimeEntry.start(id, userId, description, clock);
        timeEntries.add(entry);
        return entry;
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isMember(UserId userId) {
        return members.stream().anyMatch(m -> m.userId().equals(userId));
    }

    public boolean isManager(UserId userId) {
        return findMember(userId).map(m -> m.role().canManage()).orElse(false);
    }

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

    public ProjectId            id()          { return id; }
    public String               name()        { return name; }
    public UserId               ownerId()     { return ownerId; }
    public Instant              createdAt()   { return createdAt; }
    public int                  memberCount() { return members.size(); }
    public int                  taskCount()   { return taskIds.size(); }

    public List<ProjectMember>  members()     { return Collections.unmodifiableList(members); }
    public List<TaskId>         taskIds()     { return Collections.unmodifiableList(taskIds); }
    public List<TimeEntry>      timeEntries() { return Collections.unmodifiableList(timeEntries); }

    public Optional<ProjectMember> membershipOf(UserId userId) { return findMember(userId); }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project p)) return false;
        return id.equals(p.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', members=" + members.size() + "}";
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private Optional<ProjectMember> findMember(UserId userId) {
        return members.stream().filter(m -> m.userId().equals(userId)).findFirst();
    }

    private void requireManagerPermission(UserId requesterId) {
        ProjectMember requester = findMember(requesterId)
                .orElseThrow(() -> new ProjectMembershipRequiredException(
                    "L'utilisateur " + requesterId + " n'est pas membre du projet " + id
                ));
        if (!requester.role().canManage()) {
            throw new InsufficientProjectPermissionException(
                "Permission insuffisante : rôle " + requester.role().displayName()
                + " (MANAGER ou OWNER requis)"
            );
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidProjectNameException("Le nom du projet ne peut pas être vide");
        }
        if (name.length() > 150) {
            throw new InvalidProjectNameException("Le nom du projet ne peut pas dépasser 150 caractères");
        }
        return name.strip();
    }

    // -------------------------------------------------------------------------
    // Nested : ProjectMember (entity légère)
    // -------------------------------------------------------------------------

    public record ProjectMember(UserId userId, ProjectRole role, Instant joinedAt) {
        public ProjectMember {
            Objects.requireNonNull(userId,   "L'identifiant est obligatoire");
            Objects.requireNonNull(role,     "Le rôle est obligatoire");
            Objects.requireNonNull(joinedAt, "La date d'adhésion est obligatoire");
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class InvalidProjectNameException extends RuntimeException {
        public InvalidProjectNameException(String message) { super(message); }
    }

    public static final class InsufficientProjectPermissionException extends RuntimeException {
        public InsufficientProjectPermissionException(String message) { super(message); }
    }

    public static final class ProjectMembershipRequiredException extends RuntimeException {
        public ProjectMembershipRequiredException(String message) { super(message); }
    }

    public static final class DuplicateMemberException extends RuntimeException {
        public DuplicateMemberException(String message) { super(message); }
    }

    public static final class DuplicateTaskException extends RuntimeException {
        public DuplicateTaskException(String message) { super(message); }
    }

    public static final class ActiveTimeEntryExistsException extends RuntimeException {
        public ActiveTimeEntryExistsException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Event
    // -------------------------------------------------------------------------

    public record ProjectCreatedEvent(
            UUID      eventId,
            ProjectId projectId,
            UserId    ownerId,
            Instant   occurredAt
    ) implements DomainEvent {

        public ProjectCreatedEvent(ProjectId projectId, UserId ownerId, Instant occurredAt) {
            this(UUID.randomUUID(), projectId, ownerId, occurredAt);
        }
    }
}
