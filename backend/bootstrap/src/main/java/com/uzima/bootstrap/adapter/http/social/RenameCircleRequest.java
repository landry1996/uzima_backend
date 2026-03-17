package com.uzima.bootstrap.adapter.http.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO HTTP entrant : Renommage d'un Cercle de Vie.
 */
public record RenameCircleRequest(
        @NotBlank @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
        String newName
) {}
