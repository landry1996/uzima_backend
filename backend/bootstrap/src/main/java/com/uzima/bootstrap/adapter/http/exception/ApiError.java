package com.uzima.bootstrap.adapter.http.exception;

import java.time.Instant;

/**
 * DTO de réponse d'erreur standard (RFC 7807 Problem Details simplifié).
 * <p>
 * Structure unifiée pour toutes les erreurs HTTP retournées par l'API.
 * Garantit :
 * - Pas de fuite d'information sensible (stack traces, noms de tables, etc.)
 * - Format cohérent pour tous les clients front-end
 * - Code machine lisible (errorCode) + message humain lisible
 */
public record ApiError(
        int status,
        String errorCode,
        String message,
        Instant timestamp
) {
    public static ApiError of(int status, String errorCode, String message) {
        return new ApiError(status, errorCode, message, Instant.now());
    }
}
