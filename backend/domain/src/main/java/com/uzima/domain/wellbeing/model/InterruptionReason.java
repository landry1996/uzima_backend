package com.uzima.domain.wellbeing.model;

/**
 * Enum : Raison d'interruption d'une session de focus.
 */
public enum InterruptionReason {

    NOTIFICATION("Notification reçue"),
    USER_CHOICE("Choix de l'utilisateur"),
    EMERGENCY("Urgence"),
    TIMEOUT("Délai dépassé"),
    INCOMING_CALL("Appel entrant");

    private final String displayName;

    InterruptionReason(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
