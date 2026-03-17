package com.uzima.security.token.model;

import java.util.Objects;

/**
 * Value Object : Paire Access Token + Refresh Token.
 * Retournée après authentification réussie ou rotation.
 * Immuable par record.
 */
public record TokenPair(AccessToken accessToken, RefreshToken refreshToken) {

    public TokenPair {
        Objects.requireNonNull(accessToken, "L'access token est obligatoire");
        Objects.requireNonNull(refreshToken, "Le refresh token est obligatoire");
    }
}
