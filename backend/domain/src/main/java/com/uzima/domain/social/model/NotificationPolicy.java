package com.uzima.domain.social.model;

/**
 * Enum : Politique de notification pour un Cercle de Vie.
 * <p>
 * Détermine comment les notifications de ce cercle sont acheminées
 * vers le NotificationRouter de l'infrastructure.
 * <p>
 * IMMEDIATE   : Toutes les notifications envoyées immédiatement (default FAMILY, CLOSE_FRIENDS)
 * DEFERRED    : Notifications regroupées en batches (mode économie, default COMMUNITY)
 * URGENT_ONLY : Seules les notifications urgentes sont immédiates (default WORK)
 * BLOCKED     : Aucune notification (mode silence, géré par le bien-être numérique)
 */
public enum NotificationPolicy {

    IMMEDIATE("Immédiat"),
    DEFERRED("Différé"),
    URGENT_ONLY("Urgences uniquement"),
    BLOCKED("Bloqué");

    private final String displayName;

    NotificationPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
