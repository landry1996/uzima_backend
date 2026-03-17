package com.uzima.bootstrap.adapter.http.dto;

/**
 * DTO HTTP entrant : Révocation de tous les tokens (logout global).
 * Le userId est extrait du JWT dans le filtre — pas besoin de le passer en body.
 */
public record TokenRevokeRequest() {}
