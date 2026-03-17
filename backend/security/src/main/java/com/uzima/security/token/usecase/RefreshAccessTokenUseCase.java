package com.uzima.security.token.usecase;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.model.TokenPair;
import com.uzima.security.token.port.AccessTokenGeneratorPort;
import com.uzima.security.token.port.RefreshTokenHasherPort;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.uzima.security.token.model.TokenId.generate;

/**
 * Use Case : Rotation du Refresh Token.
 * Flux :
 * 1. Le client envoie son refresh token brut
 * 2. On hache la valeur et on cherche en base
 * 3. Si le token est RÉVOQUÉ → replay attack détectée → révocation de TOUTE la famille
 * 4. Si le token est expiré → erreur
 * 5. Si le token est valide → révocation de l'ancien, génération d'une nouvelle paire
 *    (même famille, nouveau tokenId)
 * Sécurité : Token Rotation + Token Family (défense en profondeur).
 * Pas de Spring. Pas de framework.
 */
public final class RefreshAccessTokenUseCase {

    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(30);

    private final AccessTokenGeneratorPort accessTokenGenerator;
    private final RefreshTokenHasherPort refreshTokenHasher;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TimeProvider clock;

    public RefreshAccessTokenUseCase(
            AccessTokenGeneratorPort accessTokenGenerator,
            RefreshTokenHasherPort refreshTokenHasher,
            RefreshTokenRepositoryPort refreshTokenRepository,
            TimeProvider clock
    ) {
        this.accessTokenGenerator = Objects.requireNonNull(accessTokenGenerator);
        this.refreshTokenHasher = Objects.requireNonNull(refreshTokenHasher);
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * @param rawRefreshToken Valeur brute du refresh token envoyée par le client
     * @return Nouvelle paire de tokens
     * @throws TokenRefreshException si le token est invalide, expiré ou replay détecté
     */
    public TokenPair execute(String rawRefreshToken) {
        Objects.requireNonNull(rawRefreshToken, "Le refresh token est obligatoire");
        if (rawRefreshToken.isBlank()) throw new TokenRefreshException("Le refresh token ne peut pas être vide");

        String hashedValue = refreshTokenHasher.hash(rawRefreshToken);

        RefreshToken existingToken = refreshTokenRepository.findByHashedValue(hashedValue)
                .orElseThrow(() -> new TokenRefreshException("Refresh token inconnu"));

        Instant now = clock.now();

        // REPLAY ATTACK DETECTION : token révoqué mais présenté à nouveau
        if (existingToken.isRevoked()) {
            // Audit : compter les tokens compromis dans la famille avant révocation
            List<RefreshToken> compromisedTokens = refreshTokenRepository.findByFamily(existingToken.family());
            refreshTokenRepository.revokeFamily(existingToken.family());
            throw new TokenRefreshException(
                "Refresh token compromis — " + compromisedTokens.size() + " session(s) révoquée(s)"
            );
        }

        // isUsable() combine !revoked && !isExpiredAt — le cas révoqué étant déjà traité ci-dessus,
        // cela revient à vérifier que le token n'est pas expiré
        if (!existingToken.isUsable(now)) {
            throw new TokenRefreshException("Refresh token expiré");
        }

        // Révoquer l'ancien token
        existingToken.revoke(now);
        refreshTokenRepository.update(existingToken);

        // Générer un nouveau token dans la MÊME famille (rotation)
        var newAccessToken = accessTokenGenerator.generate(existingToken.userId());

        String newRawValue = refreshTokenHasher.generateRawValue();
        String newHashedValue = refreshTokenHasher.hash(newRawValue);

        var newRefreshToken = RefreshToken.issue(
                generate(),
                existingToken.family(), // même famille
                existingToken.userId(),
                newHashedValue,
                now,
                now.plus(REFRESH_TOKEN_DURATION)
        );

        refreshTokenRepository.save(newRefreshToken);

        // Token pour le client (valeur brute)
        var clientRefreshToken = RefreshToken.issue(
                newRefreshToken.tokenId(),
                newRefreshToken.family(),
                existingToken.userId(),
                newRawValue,
                now,
                now.plus(REFRESH_TOKEN_DURATION)
        );

        return new TokenPair(newAccessToken, clientRefreshToken);
    }

    public static final class TokenRefreshException extends RuntimeException {
        public TokenRefreshException(String message) {
            super(message);
        }
    }
}
