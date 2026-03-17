package com.uzima.infrastructure.security;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.TokenClaims;
import com.uzima.security.token.model.TokenId;
import com.uzima.security.token.port.AccessTokenVerifierPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

/**
 * Adaptateur : Vérification des access tokens JWT (JJWT).
 * Implémente AccessTokenVerifierPort du module security.
 * JJWT est confiné ici. Pas de Spring. Pas de logique métier.
 */
public final class JwtAccessTokenVerifier implements AccessTokenVerifierPort {

    private final SecretKey secretKey;

    public JwtAccessTokenVerifier(String secretKeyString) {
        Objects.requireNonNull(secretKeyString, "La clé secrète JWT est obligatoire");
        if (secretKeyString.length() < 32) {
            throw new IllegalArgumentException("La clé secrète JWT doit contenir au moins 32 caractères");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TokenClaims verify(String rawToken) {
        Objects.requireNonNull(rawToken, "Le token ne peut pas être nul");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(rawToken)
                    .getPayload();

            TokenId tokenId = TokenId.of(claims.getId());
            UserId userId = UserId.of(claims.getSubject());
            Instant issuedAt = claims.getIssuedAt().toInstant();
            Instant expiresAt = claims.getExpiration().toInstant();

            return new TokenClaims(tokenId, userId, issuedAt, expiresAt);

        } catch (JwtException e) {
            throw new InvalidTokenException("Token JWT invalide ou expiré : " + e.getMessage(), e);
        }
    }
}
