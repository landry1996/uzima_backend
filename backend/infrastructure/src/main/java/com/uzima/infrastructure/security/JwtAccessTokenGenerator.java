package com.uzima.infrastructure.security;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.AccessToken;
import com.uzima.security.token.model.TokenId;
import com.uzima.security.token.port.AccessTokenGeneratorPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Adaptateur : Génération des access tokens JWT (JJWT).
 * Implémente AccessTokenGeneratorPort du module security.
 * JJWT est confiné ici. Pas de Spring. Pas de logique métier.
 * Le JTI (JWT ID) = tokenId, utilisé pour identifier le token.
 */
public final class JwtAccessTokenGenerator implements AccessTokenGeneratorPort {

    private static final Duration DEFAULT_VALIDITY = Duration.ofMinutes(15);

    private final SecretKey secretKey;
    private final Duration tokenValidity;

    public JwtAccessTokenGenerator(String secretKeyString, Duration tokenValidity) {
        Objects.requireNonNull(secretKeyString, "La clé secrète JWT est obligatoire");
        Objects.requireNonNull(tokenValidity, "La durée de validité est obligatoire");
        if (secretKeyString.length() < 32) {
            throw new IllegalArgumentException("La clé secrète JWT doit contenir au moins 32 caractères");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.tokenValidity = tokenValidity;
    }

    public JwtAccessTokenGenerator(String secretKeyString) {
        this(secretKeyString, DEFAULT_VALIDITY);
    }

    @Override
    public AccessToken generate(UserId userId) {
        Objects.requireNonNull(userId, "L'UserId est obligatoire");

        TokenId tokenId = TokenId.generate();
        Instant now = Instant.now();
        Instant expiry = now.plus(tokenValidity);

        String rawJwt = Jwts.builder()
                .id(tokenId.toString())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();

        return new AccessToken(tokenId, userId, rawJwt, now, expiry);
    }
}
