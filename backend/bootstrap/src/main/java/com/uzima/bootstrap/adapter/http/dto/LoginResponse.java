package com.uzima.bootstrap.adapter.http.dto;

/**
 * DTO HTTP sortant : Réponse d'authentification.
 * Retourne les deux tokens pour une session complète :
 * - accessToken  : JWT à courte durée de vie (15 min), à placer dans Authorization: Bearer
 * - refreshToken : valeur opaque à longue durée de vie (30 jours), à stocker côté client
 *   et à envoyer à POST /api/auth/token/refresh pour renouveler l'access token.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String firstName,
        String lastName,
        String phoneNumber
) {}
