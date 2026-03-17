package com.uzima.domain.assistant.model;

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
 * Aggregate Root : Rappel contextuel assistant.
 * <p>
 * Cycle de vie :
 *   PENDING  → TRIGGERED (trigger())
 *   PENDING  → DISMISSED (dismiss())
 *   TRIGGERED → SNOOZED  (snooze())
 *   TRIGGERED → DISMISSED (dismiss())
 *   SNOOZED  → TRIGGERED (trigger() après snoozedUntil)
 *   SNOOZED  → DISMISSED (dismiss())
 * <p>
 * Invariants :
 * - Contenu obligatoire, max 500 caractères
 * - Un rappel DISMISSED ne peut plus être modifié
 */
public final class Reminder {

    private final ReminderId       id;
    private final UserId           userId;
    private final String           content;
    private final ReminderTrigger  trigger;
    private final Instant          scheduledAt;
    private final Instant          createdAt;
    private       ReminderStatus   status;
    private       Instant          triggeredAt;
    private       Instant          dismissedAt;
    private       Instant          snoozedUntil;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Reminder(ReminderId id, UserId userId, String content, ReminderTrigger trigger,
                     Instant scheduledAt, Instant createdAt, ReminderStatus status,
                     Instant triggeredAt, Instant dismissedAt, Instant snoozedUntil) {
        this.id           = Objects.requireNonNull(id,          "L'identifiant est obligatoire");
        this.userId       = Objects.requireNonNull(userId,      "L'identifiant utilisateur est obligatoire");
        this.trigger      = Objects.requireNonNull(trigger,     "Le type de déclencheur est obligatoire");
        this.scheduledAt  = Objects.requireNonNull(scheduledAt, "La date de déclenchement est obligatoire");
        this.createdAt    = Objects.requireNonNull(createdAt,   "La date de création est obligatoire");
        this.status       = Objects.requireNonNull(status,      "Le statut est obligatoire");
        this.content      = validateContent(content);
        this.triggeredAt  = triggeredAt;
        this.dismissedAt  = dismissedAt;
        this.snoozedUntil = snoozedUntil;
    }

    // -------------------------------------------------------------------------
    // Factory : create()
    // -------------------------------------------------------------------------

    /**
     * Crée un nouveau rappel en état PENDING.
     *
     * @throws InvalidReminderContentException si le contenu est invalide
     */
    public static Reminder create(UserId userId, String content, ReminderTrigger trigger,
                                   Instant scheduledAt, TimeProvider clock) {
        Objects.requireNonNull(userId,      "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(trigger,     "Le type de déclencheur est obligatoire");
        Objects.requireNonNull(scheduledAt, "La date de déclenchement est obligatoire");
        Objects.requireNonNull(clock,       "Le fournisseur de temps est obligatoire");

        return new Reminder(
            ReminderId.generate(), userId, content, trigger,
            scheduledAt, clock.now(), ReminderStatus.PENDING,
            null, null, null
        );
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    public static Reminder reconstitute(
            ReminderId id, UserId userId, String content, ReminderTrigger trigger,
            Instant scheduledAt, Instant createdAt, ReminderStatus status,
            Instant triggeredAt, Instant dismissedAt, Instant snoozedUntil
    ) {
        return new Reminder(id, userId, content, trigger, scheduledAt, createdAt,
                            status, triggeredAt, dismissedAt, snoozedUntil);
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Déclenche le rappel (PENDING/SNOOZED → TRIGGERED).
     *
     * @throws IllegalReminderTransitionException si le rappel est DISMISSED
     */
    public void trigger(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireNotTerminal("déclencher");
        if (this.status == ReminderStatus.TRIGGERED) return; // idempotent
        this.status      = ReminderStatus.TRIGGERED;
        this.triggeredAt = clock.now();
        this.snoozedUntil = null;
        domainEvents.add(new ReminderTriggeredEvent(id, userId, content, triggeredAt));
    }

    /**
     * Reporte le rappel (TRIGGERED → SNOOZED).
     *
     * @param delay Durée du report
     * @throws IllegalReminderTransitionException si le rappel n'est pas TRIGGERED
     */
    public void snooze(Duration delay, TimeProvider clock) {
        Objects.requireNonNull(delay, "La durée de report est obligatoire");
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalReminderTransitionException("La durée de report doit être positive");
        }
        if (this.status != ReminderStatus.TRIGGERED) {
            throw new IllegalReminderTransitionException(
                "Impossible de reporter un rappel en état " + this.status.displayName()
            );
        }
        this.status       = ReminderStatus.SNOOZED;
        this.snoozedUntil = clock.now().plus(delay);
    }

    /**
     * Ignore le rappel (tout état non terminal → DISMISSED).
     *
     * @throws IllegalReminderTransitionException si le rappel est déjà DISMISSED
     */
    public void dismiss(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireNotTerminal("ignorer");
        this.status      = ReminderStatus.DISMISSED;
        this.dismissedAt = clock.now();
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isPending()    { return status == ReminderStatus.PENDING; }
    public boolean isDismissed()  { return status == ReminderStatus.DISMISSED; }

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

    public ReminderId      id()           { return id; }
    public UserId          userId()       { return userId; }
    public String          content()      { return content; }
    public ReminderTrigger trigger()      { return trigger; }
    public ReminderStatus  status()       { return status; }
    public Instant         scheduledAt()  { return scheduledAt; }
    public Instant         createdAt()    { return createdAt; }

    public Optional<Instant> triggeredAt()  { return Optional.ofNullable(triggeredAt); }
    public Optional<Instant> dismissedAt()  { return Optional.ofNullable(dismissedAt); }
    public Optional<Instant> snoozedUntil() { return Optional.ofNullable(snoozedUntil); }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reminder r)) return false;
        return id.equals(r.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Reminder{id=" + id + ", status=" + status.displayName()
             + ", trigger=" + trigger.displayName() + "}";
    }

    // -------------------------------------------------------------------------
    // Helper privé
    // -------------------------------------------------------------------------

    private void requireNotTerminal(String action) {
        if (this.status.isTerminal()) {
            throw new IllegalReminderTransitionException(
                "Impossible de " + action + " un rappel en état " + this.status.displayName()
                + " [état terminal]"
            );
        }
    }

    private static String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidReminderContentException("Le contenu du rappel ne peut pas être vide");
        }
        if (content.length() > 500) {
            throw new InvalidReminderContentException(
                "Le contenu du rappel ne peut pas dépasser 500 caractères"
            );
        }
        return content.strip();
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class InvalidReminderContentException extends RuntimeException {
        public InvalidReminderContentException(String message) { super(message); }
    }

    public static final class IllegalReminderTransitionException extends RuntimeException {
        public IllegalReminderTransitionException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public record ReminderTriggeredEvent(
            UUID       eventId,
            ReminderId reminderId,
            UserId     userId,
            String     content,
            Instant    occurredAt
    ) implements DomainEvent {

        public ReminderTriggeredEvent(ReminderId reminderId, UserId userId,
                                       String content, Instant occurredAt) {
            this(UUID.randomUUID(), reminderId, userId, content, occurredAt);
        }
    }
}
