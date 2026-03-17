package com.uzima.bootstrap.adapter.http.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO HTTP entrant : Ajout d'un membre dans un Cercle de Vie.
 */
public record AddMemberRequest(
        @NotBlank(message = "L'identifiant du membre est obligatoire")
        String newMemberId,

        @NotNull(message = "Le rôle est obligatoire")
        String role
) {}
