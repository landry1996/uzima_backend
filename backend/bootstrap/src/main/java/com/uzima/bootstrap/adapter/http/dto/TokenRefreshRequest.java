package com.uzima.bootstrap.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO HTTP entrant : Renouvellement du token via refresh token.
 */
public record TokenRefreshRequest(
        @NotBlank(message = "Le refresh token est obligatoire")
        String refreshToken
) {}
