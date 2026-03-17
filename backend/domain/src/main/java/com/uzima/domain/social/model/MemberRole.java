package com.uzima.domain.social.model;

/**
 * Enum : Rôle d'un membre dans un Cercle de Vie.
 * <p>
 * Hiérarchie des permissions (ordre croissant) :
 *   GUEST < MEMBER < ADMIN < OWNER
 * <p>
 * OWNER : Créateur du cercle. Unique, non transférable. Peut tout faire.
 *          Ne peut pas quitter le cercle sans le supprimer ou transférer.
 * ADMIN : Peut ajouter/retirer des membres, modifier les règles du cercle.
 * MEMBER : Membre standard. Peut voir et interagir selon les règles.
 * GUEST : Accès temporaire en lecture seule. Ne peut pas interagir.
 */
public enum MemberRole {

    GUEST("Invité"),
    MEMBER("Membre"),
    ADMIN("Administrateur"),
    OWNER("Propriétaire");

    private final String displayName;

    MemberRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Retourne true si ce rôle est au moins ADMIN (ADMIN ou OWNER). */
    public boolean isAtLeastAdmin() {
        return this == ADMIN || this == OWNER;
    }
}
