package com.uzima.domain.workspace.model;

/**
 * Enum : États du cycle de vie d'une tâche (Kanban).
 * <p>
 * Transitions autorisées :
 *   BACKLOG → TODO         (planification)
 *   TODO    → IN_PROGRESS  (démarrage — start())
 *   IN_PROGRESS → IN_REVIEW (soumission pour revue)
 *   IN_REVIEW   → IN_PROGRESS (retour en cours)
 *   IN_REVIEW   → DONE        (validation — complete())
 *   IN_PROGRESS → DONE        (complete() sans revue)
 *   DONE        → TODO        (réouverture — reopen())
 *   Tout état non-DONE → bloqué (block(), sans changement de statut)
 */
public enum TaskStatus {

    BACKLOG("En attente"),
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    IN_REVIEW("En revue"),
    DONE("Terminée");

    private final String displayName;

    TaskStatus(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    /** Retourne true si la tâche est dans un état terminal. */
    public boolean isTerminal() { return this == DONE; }

    /** Retourne true si la tâche est active (en cours de traitement). */
    public boolean isActive() { return this == IN_PROGRESS || this == IN_REVIEW; }
}
