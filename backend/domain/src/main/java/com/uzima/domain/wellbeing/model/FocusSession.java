package com.uzima.domain.wellbeing.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Duration;
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
 * Aggregate Root : Session de focus (concentration sans distraction).
 * <p>
 * Cycle de vie :
 *   ACTIVE → COMPLETED  (end())
 *   ACTIVE → INTERRUPTED (interrupt(reason))
 * <p>
 * Invariants :
 * - Une session COMPLETED ou INTERRUPTED ne peut plus être modifiée
 * - endedAt >= startedAt
 */
public final class FocusSession {

    private final FocusSessionId        id;
    private final UserId                userId;
    private final Instant               startedAt;
    private       FocusSessionStatus    status;
    private       Instant               endedAt;
    private       InterruptionReason    interruptionReason;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private FocusSession(FocusSessionId id, UserId userId, Instant startedAt,
                         FocusSessionStatus status, Instant endedAt,
                         InterruptionReason interruptionReason) {
        this.id                 = Objects.requireNonNull(id,        "L'identifiant est obligatoire");
        this.userId             = Objects.requireNonNull(userId,    "L'identifiant utilisateur est obligatoire");
        this.startedAt          = Objects.requireNonNull(startedAt, "La date de début est obligatoire");
        this.status             = Objects.requireNonNull(status,    "Le statut est obligatoire");
        this.endedAt            = endedAt;
        this.interruptionReason = interruptionReason;
    }

    // -------------------------------------------------------------------------
    // Factory : start()
    // -------------------------------------------------------------------------

    /**
     * Démarre une nouvelle session de focus.
     */
    public static FocusSession start(UserId userId, TimeProvider clock) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");
        return new FocusSession(
            FocusSessionId.generate(), userId, clock.now(),
            FocusSessionStatus.ACTIVE, null, null
        );
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    public static FocusSession reconstitute(
            FocusSessionId id, UserId userId, Instant startedAt,
            FocusSessionStatus status, Instant endedAt,
            InterruptionReason interruptionReason
    ) {
        return new FocusSession(id, userId, startedAt, status, endedAt, interruptionReason);
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Termine la session normalement (ACTIVE → COMPLETED).
     *
     * @throws AlreadyEndedException si la session est déjà terminée
     */
    public void end(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireActive("terminer");
        this.status  = FocusSessionStatus.COMPLETED;
        this.endedAt = clock.now();
        domainEvents.add(new FocusSessionEndedEvent(id, userId, startedAt, endedAt));
    }

    /**
     * Interrompt la session (ACTIVE → INTERRUPTED).
     *
     * @param reason Raison de l'interruption
     * @throws AlreadyEndedException si la session est déjà terminée
     */
    public void interrupt(InterruptionReason reason, TimeProvider clock) {
        Objects.requireNonNull(reason, "La raison d'interruption est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");
        requireActive("interrompre");
        this.status             = FocusSessionStatus.INTERRUPTED;
        this.endedAt            = clock.now();
        this.interruptionReason = reason;
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isActive()      { return status == FocusSessionStatus.ACTIVE; }
    public boolean isCompleted()   { return status == FocusSessionStatus.COMPLETED; }
    public boolean wasInterrupted(){ return status == FocusSessionStatus.INTERRUPTED; }

    // -------------------------------------------------------------------------
    // Computed
    // -------------------------------------------------------------------------

    /**
     * Durée effective de la session. Vide si la session est encore ACTIVE.
     */
    public Optional<Duration> duration() {
        if (endedAt == null) return Optional.empty();
        return Optional.of(Duration.between(startedAt, endedAt));
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

    public FocusSessionId            id()                  { return id; }
    public UserId                    userId()              { return userId; }
    public Instant                   startedAt()           { return startedAt; }
    public FocusSessionStatus        status()              { return status; }
    public Optional<Instant>         endedAt()             { return Optional.ofNullable(endedAt); }
    public Optional<InterruptionReason> interruptionReason(){ return Optional.ofNullable(interruptionReason); }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FocusSession s)) return false;
        return id.equals(s.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "FocusSession{id=" + id + ", status=" + status + "}";
    }

    // -------------------------------------------------------------------------
    // Helper privé
    // -------------------------------------------------------------------------

    private void requireActive(String action) {
        if (this.status != FocusSessionStatus.ACTIVE) {
            throw new AlreadyEndedException(
                "Impossible de " + action + " une session déjà terminée (statut : " + status + ")"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class AlreadyEndedException extends RuntimeException {
        public AlreadyEndedException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public record FocusSessionEndedEvent(
            UUID            eventId,
            FocusSessionId  sessionId,
            UserId          userId,
            Instant         startedAt,
            Instant         endedAt,
            Instant         occurredAt
    ) implements DomainEvent {

        public FocusSessionEndedEvent(FocusSessionId sessionId, UserId userId,
                                      Instant startedAt, Instant endedAt) {
            this(UUID.randomUUID(), sessionId, userId, startedAt, endedAt, endedAt);
        }
    }
}
