package com.uzima.domain.workspace.model;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Entity : Entrée de temps (time tracking) liée à un projet.
 * <p>
 * Cycle de vie :
 *   RUNNING (startedAt fixé, stoppedAt null)
 *   STOPPED (startedAt + stoppedAt fixés)
 * <p>
 * Appartient au Project (cycle de vie géré par ProjectRepository).
 * Constructeur package-private — instancié uniquement via Project.startTimeEntry() ou reconstitution.
 */
public final class TimeEntry {

    private final TimeEntryId id;
    private final UserId      userId;
    private final ProjectId   projectId;
    private final String      description;
    private final Instant     startedAt;
    private       Instant     stoppedAt;

    // -------------------------------------------------------------------------
    // Constructeur package-private
    // -------------------------------------------------------------------------

    TimeEntry(TimeEntryId id, UserId userId, ProjectId projectId, String description,
              Instant startedAt, Instant stoppedAt) {
        this.id          = Objects.requireNonNull(id,        "L'identifiant est obligatoire");
        this.userId      = Objects.requireNonNull(userId,    "L'identifiant utilisateur est obligatoire");
        this.projectId   = Objects.requireNonNull(projectId, "L'identifiant de projet est obligatoire");
        this.startedAt   = Objects.requireNonNull(startedAt, "La date de début est obligatoire");
        this.description = description;
        this.stoppedAt   = stoppedAt;
    }

    // -------------------------------------------------------------------------
    // Factory (appelée par Project)
    // -------------------------------------------------------------------------

    static TimeEntry start(ProjectId projectId, UserId userId, String description, TimeProvider clock) {
        Objects.requireNonNull(projectId, "L'identifiant de projet est obligatoire");
        Objects.requireNonNull(userId,    "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(clock,     "Le fournisseur de temps est obligatoire");
        return new TimeEntry(TimeEntryId.generate(), userId, projectId, description, clock.now(), null);
    }

    public static TimeEntry reconstitute(TimeEntryId id, UserId userId, ProjectId projectId,
                                          String description, Instant startedAt, Instant stoppedAt) {
        return new TimeEntry(id, userId, projectId, description, startedAt, stoppedAt);
    }

    // -------------------------------------------------------------------------
    // Comportements
    // -------------------------------------------------------------------------

    /**
     * Arrête le chronomètre.
     *
     * @throws AlreadyStoppedException si l'entrée est déjà arrêtée
     */
    public void stop(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (stoppedAt != null) {
            throw new AlreadyStoppedException("Cette entrée de temps est déjà arrêtée : " + id);
        }
        this.stoppedAt = clock.now();
    }

    // -------------------------------------------------------------------------
    // Calculs
    // -------------------------------------------------------------------------

    /**
     * Retourne la durée calculée entre startedAt et stoppedAt.
     * Si encore en cours (stoppedAt == null) : retourne empty.
     */
    public Optional<Duration> duration() {
        if (stoppedAt == null) return Optional.empty();
        return Optional.of(Duration.between(startedAt, stoppedAt));
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isRunning() { return stoppedAt == null; }
    public boolean isStopped() { return stoppedAt != null; }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public TimeEntryId      id()          { return id; }
    public UserId           userId()      { return userId; }
    public ProjectId        projectId()   { return projectId; }
    public Instant          startedAt()   { return startedAt; }
    public Optional<String>  description() { return Optional.ofNullable(description); }
    public Optional<Instant> stoppedAt()   { return Optional.ofNullable(stoppedAt); }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeEntry t)) return false;
        return id.equals(t.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class AlreadyStoppedException extends RuntimeException {
        public AlreadyStoppedException(String message) { super(message); }
    }
}
