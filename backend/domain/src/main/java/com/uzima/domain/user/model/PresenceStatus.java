package com.uzima.domain.user.model;

/**
 * Enumération des états de présence d'un utilisateur.
 * Chaque état porte sa propre sémantique métier (Nouveauté 6 du cahier des charges).
 * - AVAILABLE : Disponible, notifications normales
 * - FOCUSED : Concentré (Deep Work), notifications différées sauf urgences
 * - TIRED : Fatigué, suggère messages courts, pas de vocaux longs
 * - TRAVELING : En déplacement, réponses vocales uniquement
 * - SILENCE : Besoin de silence, mode texte uniquement, pas d'appels
 * - PHYSICAL_ACTIVITY : Activité physique, urgences uniquement
 * - WELLNESS : Bien-être / méditation, notifications bloquées 100%
 * - SLEEPING : Sommeil, urgences filtrées (famille seulement)
 * - CELEBRATING   : Mode festif, suggestions réponses joyeuses
 * - OFFLINE : Hors ligne
 */
public enum PresenceStatus {

    AVAILABLE("Disponible", NotificationPolicy.NORMAL),
    FOCUSED("Concentré", NotificationPolicy.DEFERRED),
    TIRED("Fatigué", NotificationPolicy.DEFERRED),
    TRAVELING("En déplacement", NotificationPolicy.URGENT_ONLY),
    SILENCE("Silence", NotificationPolicy.DEFERRED),
    PHYSICAL_ACTIVITY("Activité physique", NotificationPolicy.URGENT_ONLY),
    WELLNESS("Bien-être", NotificationPolicy.BLOCKED),
    SLEEPING("Sommeil", NotificationPolicy.URGENT_ONLY),
    CELEBRATING("Célébration", NotificationPolicy.NORMAL),
    OFFLINE("Hors ligne", NotificationPolicy.BLOCKED);

    private final String displayName;
    private final NotificationPolicy notificationPolicy;

    PresenceStatus(String displayName, NotificationPolicy notificationPolicy) {
        this.displayName = displayName;
        this.notificationPolicy = notificationPolicy;
    }

    public String displayName() {
        return displayName;
    }

    public NotificationPolicy notificationPolicy() {
        return notificationPolicy;
    }

    public boolean allowsVoiceMessages() {
        return this != SILENCE && this != WELLNESS && this != SLEEPING;
    }

    public boolean allowsPhoneCalls() {
        return this == AVAILABLE || this == CELEBRATING;
    }

    /**
     * Politique de notification selon l'état de présence.
     * Utilisée par l'infrastructure pour décider comment router les notifications.
     */
    public enum NotificationPolicy {
        /** Notifications immédiates normales */
        NORMAL,
        /** Notifications différées (batching) */
        DEFERRED,
        /** Uniquement les urgences (famille, contacts prioritaires) */
        URGENT_ONLY,
        /** Aucune notification */
        BLOCKED
    }
}
