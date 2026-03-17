package com.uzima.domain.workspace.model;

/**
 * Enum : Niveau de priorité d'une tâche.
 * Ordre croissant : LOW < MEDIUM < HIGH < CRITICAL
 */
public enum TaskPriority {

    LOW("Faible"),
    MEDIUM("Moyenne"),
    HIGH("Haute"),
    CRITICAL("Critique");

    private final String displayName;

    TaskPriority(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

}
