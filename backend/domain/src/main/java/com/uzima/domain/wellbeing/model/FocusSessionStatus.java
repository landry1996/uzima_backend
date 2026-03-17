package com.uzima.domain.wellbeing.model;

/** Enum : Statut d'une session de focus. */
public enum FocusSessionStatus {

    ACTIVE("En cours"),
    COMPLETED("Terminée"),
    INTERRUPTED("Interrompue");

    private final String displayName;

    FocusSessionStatus(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public boolean isTerminal() { return this == COMPLETED || this == INTERRUPTED; }
}
