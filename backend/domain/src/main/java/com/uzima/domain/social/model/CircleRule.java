package com.uzima.domain.social.model;

import java.util.Objects;

/**
 * Value Object : Règles de comportement d'un Cercle de Vie.
 * Immuable (record Java). Regroupe toutes les politiques configurables
 * par un ADMIN ou OWNER pour personnaliser l'expérience du cercle.
 * Invariants :
 * - notificationPolicy != null
 * - visibility != null
 */
public record CircleRule(
        NotificationPolicy notificationPolicy,
        VisibilityLevel    visibility,
        boolean            allowsVoiceMessages,
        boolean            allowsPayments
) {

    public CircleRule {
        Objects.requireNonNull(notificationPolicy, "La politique de notification est obligatoire");
        Objects.requireNonNull(visibility, "Le niveau de visibilité est obligatoire");
    }

    // -------------------------------------------------------------------------
    // Factory : règles par défaut selon le type de cercle
    // -------------------------------------------------------------------------

    /**
     * Crée les règles par défaut adaptées à un type de cercle.
     *
     * <pre>
     * FAMILY        → IMMEDIATE, CIRCLE_ONLY,  voix=true,  paiements=true
     * WORK          → URGENT_ONLY, CIRCLE_ONLY, voix=false, paiements=true
     * CLOSE_FRIENDS → IMMEDIATE, CIRCLE_ONLY,  voix=true,  paiements=true
     * PROJECT       → URGENT_ONLY, CIRCLE_ONLY, voix=true,  paiements=true
     * COMMUNITY     → DEFERRED,  PUBLIC,        voix=false, paiements=false
     * </pre>
     */
    public static CircleRule defaultForType(CircleType type) {
        Objects.requireNonNull(type, "Le type de cercle est obligatoire");
        return switch (type) {
            case FAMILY, CLOSE_FRIENDS -> new CircleRule(NotificationPolicy.IMMEDIATE,    VisibilityLevel.CIRCLE_ONLY, true,  true);
            case WORK          -> new CircleRule(NotificationPolicy.URGENT_ONLY,  VisibilityLevel.CIRCLE_ONLY, false, true);
            case PROJECT       -> new CircleRule(NotificationPolicy.URGENT_ONLY,  VisibilityLevel.CIRCLE_ONLY, true,  true);
            case COMMUNITY     -> new CircleRule(NotificationPolicy.DEFERRED,     VisibilityLevel.PUBLIC,      false, false);
        };
    }

}
