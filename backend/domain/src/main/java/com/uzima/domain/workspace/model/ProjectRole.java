package com.uzima.domain.workspace.model;

/**
 * Enum : Rôle d'un membre dans un Project workspace.
 * <p>
 * Domaine independant de social.MemberRole pour éviter le couplage inter-domaines.
 * <p>
 * OWNER : Créateur du projet, droits complets.
 * MANAGER : Peut gérer les tâches et les membres.
 * MEMBER : Peut créer et modifier des tâches qui lui sont assignées.
 * VIEWER : Lecture seule — tableau Kanban uniquement.
 */
public enum ProjectRole {

    VIEWER("Observateur"),
    MEMBER("Membre"),
    MANAGER("Manager"),
    OWNER("Propriétaire");

    private final String displayName;

    ProjectRole(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public boolean canManage() { return this == MANAGER || this == OWNER; }
}
