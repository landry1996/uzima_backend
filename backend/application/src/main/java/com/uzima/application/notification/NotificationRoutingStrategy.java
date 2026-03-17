package com.uzima.application.notification;

import com.uzima.domain.user.model.PresenceStatus;

/**
 * Port (Strategy) : Stratégie de routage des notifications.
 * Justification du pattern Strategy (directement issue du cahier des charges) :
 * Nouveauté 7 : "Respect Automatique États" :
 * - AVAILABLE/CELEBRATING → notification immédiate
 * - FOCUSED/TIRED/SILENCE → notification différée (batching)
 * - TRAVELING/PHYSICAL_ACTIVITY/SLEEPING → urgences uniquement
 * - WELLNESS/OFFLINE → bloqué
 * Sans Strategy : un if/else géant avec 10 branches dans SendMessageUseCase.
 * Avec Strategy : chaque politique est encapsulée, testée, remplaçable indépendamment.
 * Appartient à l'application (pas au domaine) car elle orchestre des ports d'infrastructure.
 * Implémentée dans la couche infrastructure (WebSocket, Redis queue, etc.).
 */
public interface NotificationRoutingStrategy {

    /**
     * Route la notification selon la politique de cet état.
     *
     * @param notification La notification à router
     */
    void route(PendingNotification notification);

    /**
     * La politique de notification que cette stratégie gère.
     * Utilisée par NotificationRouter pour sélectionner la bonne stratégie.
     */
    PresenceStatus.NotificationPolicy supportedPolicy();
}
