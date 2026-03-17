package com.uzima.domain.social.model;

/**
 * Enum : Types de Cercles de Vie dans Uzima.
 * Chaque type détermine les règles par défaut (voir CircleRule.defaultForType).
 * <p>
 * FAMILY : Famille proche — visibilité restreinte, paiements activés, voix activé
 * WORK : Collègues / clients — visibilité cercle uniquement, pas de voix par défaut
 * CLOSE_FRIENDS : Amis proches — visibilité restreinte, voix activé
 * PROJECT : Projet temporaire (groupe de travail, event) — visibilité cercle, tout activé
 * COMMUNITY : Communauté ouverte (quartier, association) — visibilité publique
 */
public enum CircleType {

    FAMILY("Famille"),
    WORK("Travail"),
    CLOSE_FRIENDS("Amis proches"),
    PROJECT("Projet"),
    COMMUNITY("Communauté");

    private final String displayName;

    CircleType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
