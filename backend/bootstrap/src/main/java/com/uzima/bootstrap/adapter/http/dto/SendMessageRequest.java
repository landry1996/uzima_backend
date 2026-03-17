package com.uzima.bootstrap.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO HTTP entrant : Envoi de message.
 */
public record SendMessageRequest(
        @NotBlank(message = "Le contenu du message est obligatoire")
        @Size(max = 4096, message = "Le message ne peut pas dépasser 4096 caractères")
        String content
) {}
