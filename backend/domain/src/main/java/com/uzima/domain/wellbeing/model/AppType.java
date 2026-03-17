package com.uzima.domain.wellbeing.model;

/**
 * Enum : Type d'application utilisée lors d'une session.
 * <p>
 * Utilisé pour calculer les métriques de santé digitale
 * (temps d'écran par catégorie, usage PRODUCTIF vs DISTRACTION).
 */
public enum AppType {

    SOCIAL("Réseau social"),
    MESSAGING("Messagerie"),
    ENTERTAINMENT("Divertissement"),
    PRODUCTIVITY("Productivité"),
    WORK("Travail"),
    HEALTH("Santé"),
    OTHER("Autre");

    private final String displayName;

    AppType(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    /** Retourne true si ce type d'app contribue positivement à la santé digitale. */
    public boolean isProductive() {
        return this == PRODUCTIVITY || this == WORK || this == HEALTH;
    }
}
