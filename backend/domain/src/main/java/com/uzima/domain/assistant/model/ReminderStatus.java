package com.uzima.domain.assistant.model;

/**
 * Enum : États du cycle de vie d'un rappel.
 * <p>
 * PENDING   → TRIGGERED (trigger())
 * PENDING   → DISMISSED (dismiss())
 * TRIGGERED → SNOOZED   (snooze())
 * TRIGGERED → DISMISSED (dismiss())
 * SNOOZED   → TRIGGERED (trigger() après snoozedUntil)
 * SNOOZED   → DISMISSED (dismiss())
 * <p>
 * États terminaux : DISMISSED
 */
public enum ReminderStatus {

    PENDING("En attente"),
    TRIGGERED("Déclenché"),
    SNOOZED("Reporté"),
    DISMISSED("Ignoré");

    private final String displayName;

    ReminderStatus(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public boolean isTerminal() { return this == DISMISSED; }

    public boolean isActive() { return this == PENDING || this == SNOOZED; }
}
