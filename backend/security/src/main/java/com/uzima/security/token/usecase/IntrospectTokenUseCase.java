package com.uzima.security.token.usecase;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.security.token.model.AccessToken;
import com.uzima.security.token.model.TokenClaims;
import com.uzima.security.token.port.AccessTokenVerifierPort;

import java.util.Objects;

/**
 * Use Case : Introspection d'un access token JWT.
 * <p>
 * Permet à un client (application mobile, SPA) de vérifier la validité de son token
 * et d'obtenir ses métadonnées sans effectuer d'appel métier.
 * <p>
 * Si le token est invalide ou expiré, {@link AccessTokenVerifierPort.InvalidTokenException} est propagée
 * et interceptée par le GlobalExceptionHandler → HTTP 401.
 */
public final class IntrospectTokenUseCase {

    private final AccessTokenVerifierPort tokenVerifier;
    private final TimeProvider clock;

    public IntrospectTokenUseCase(AccessTokenVerifierPort tokenVerifier, TimeProvider clock) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "Le vérificateur de token est obligatoire");
        this.clock         = Objects.requireNonNull(clock,         "Le fournisseur de temps est obligatoire");
    }

    /**
     * Vérifie et introspecte un access token brut.
     *
     * @param rawToken Valeur brute du JWT (sans préfixe "Bearer ")
     * @return L'access token reconstitué avec ses métadonnées
     * @throws AccessTokenVerifierPort.InvalidTokenException si le token est invalide, expiré ou falsifié
     */
    public AccessToken execute(String rawToken) {
        Objects.requireNonNull(rawToken, "Le token est obligatoire");
        if (rawToken.isBlank()) {
            throw new AccessTokenVerifierPort.InvalidTokenException("Le token ne peut pas être vide");
        }

        // Vérifie signature, expiration et intégrité — lance InvalidTokenException si invalide
        TokenClaims claims = tokenVerifier.verify(rawToken);

        AccessToken accessToken = new AccessToken(
            claims.tokenId(),
            claims.userId(),
            rawToken,
            claims.issuedAt(),
            claims.expiresAt()
        );

        // Vérification défensive post-parsing : le token ne doit pas être expiré
        if (accessToken.isExpiredAt(clock.now())) {
            throw new AccessTokenVerifierPort.InvalidTokenException("Access token expiré");
        }

        return accessToken;
    }
}
