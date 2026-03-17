package com.uzima.domain.social.model;

/**
 * Enum : Niveau de visibilité des contenus d'un Cercle de Vie.
 * <p>
 * PUBLIC       : Visible par tous les utilisateurs Uzima
 * CIRCLE_ONLY  : Visible uniquement par les membres du cercle
 * PRIVATE      : Visible uniquement par le propriétaire (OWNER) et les ADMINs
 */
public enum VisibilityLevel {

    PUBLIC("Public"),
    CIRCLE_ONLY("Cercle uniquement"),
    PRIVATE("Privé");

    private final String displayName;

    VisibilityLevel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

}
