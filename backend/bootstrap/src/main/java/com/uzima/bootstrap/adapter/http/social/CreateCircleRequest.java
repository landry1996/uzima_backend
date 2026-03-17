package com.uzima.bootstrap.adapter.http.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO HTTP entrant : Création d'un Cercle de Vie.
 */
public record CreateCircleRequest(
        @NotBlank @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
        String name,

        @NotNull(message = "Le type de cercle est obligatoire")
        String type
) {}
