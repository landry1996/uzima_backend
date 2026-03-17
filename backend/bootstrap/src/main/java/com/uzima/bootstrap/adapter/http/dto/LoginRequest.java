package com.uzima.bootstrap.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO HTTP entrant : Connexion.
 */
public record LoginRequest(
        @NotBlank String phoneNumber,
        @NotBlank String password
) {}
