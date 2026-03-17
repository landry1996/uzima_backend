package com.uzima.domain.assistant.model;

/**
 * Enum : Type de déclencheur d'un rappel.
 * <p>
 * - TIME_BASED : déclenché à une heure planifiée
 * - LOCATION_BASED : déclenché à l'entrée/sortie d'une zone géographique
 * - CONTEXT_BASED : déclenché par un contexte détecté (calendrier, intention, ML)
 */
public enum ReminderTrigger {

    TIME_BASED("Basé sur l'heure"),
    LOCATION_BASED("Basé sur la localisation"),
    CONTEXT_BASED("Basé sur le contexte");

    private final String displayName;

    ReminderTrigger(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
