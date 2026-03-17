package com.uzima.bootstrap.adapter.http.dto;

import com.uzima.security.token.model.TokenPair;

/**
 * DTO HTTP sortant : Paire de tokens après refresh.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken
) {
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(
                pair.accessToken().rawValue(),
                pair.refreshToken().hashedValue() // rawValue côté client (voir GenerateTokenPairUseCase)
        );
    }
}
