package com.uzima.bootstrap.adapter.http.social;

import jakarta.validation.constraints.NotNull;

/**
 * DTO HTTP entrant : Mise à jour des règles d'un Cercle de Vie.
 */
public record UpdateCircleRulesRequest(
        @NotNull(message = "La politique de notification est obligatoire")
        String notificationPolicy,

        @NotNull(message = "Le niveau de visibilité est obligatoire")
        String visibility,

        @NotNull(message = "allowsVoiceMessages est obligatoire")
        Boolean allowsVoiceMessages,

        @NotNull(message = "allowsPayments est obligatoire")
        Boolean allowsPayments
) {}
