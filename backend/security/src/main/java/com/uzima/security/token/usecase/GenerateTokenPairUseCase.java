package com.uzima.security.token.usecase;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.model.TokenFamily;
import com.uzima.security.token.model.TokenPair;
import com.uzima.security.token.port.AccessTokenGeneratorPort;
import com.uzima.security.token.port.RefreshTokenHasherPort;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static com.uzima.security.token.model.TokenId.generate;

/**
 * Use Case : Génération d'une paire Access + Refresh Token.
 * Appelé après une authentification réussie (login).
 * Crée une nouvelle TokenFamily pour cette session.
 * Pas de Spring. Pas de framework.
 */
public final class GenerateTokenPairUseCase {

    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(30);

    private final AccessTokenGeneratorPort accessTokenGenerator;
    private final RefreshTokenHasherPort refreshTokenHasher;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TimeProvider clock;

    public GenerateTokenPairUseCase(
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
     * Génère une nouvelle paire de tokens pour l'utilisateur.
     * Une nouvelle famille est créée (première connexion ou après révocation globale).
     *
     * @param userId Identifiant de l'utilisateur authentifié
     * @return Paire de tokens à retourner au client
     */
    public TokenPair execute(UserId userId) {
        Objects.requireNonNull(userId, "Le userId est obligatoire");

        var accessToken = accessTokenGenerator.generate(userId);

        String rawRefreshValue = refreshTokenHasher.generateRawValue();
        String hashedRefreshValue = refreshTokenHasher.hash(rawRefreshValue);

        Instant now = clock.now();
        var refreshToken = RefreshToken.issue(
                generate(),
                TokenFamily.generate(),
                userId,
                hashedRefreshValue,
                now,
                now.plus(REFRESH_TOKEN_DURATION)
        );

        refreshTokenRepository.save(refreshToken);

        // On retourne un RefreshToken "vue cliente" avec la valeur RAW (non hachée)
        // Ce RefreshToken n'est PAS celui stocké en base (qui contient le hash)
        var clientRefreshToken = RefreshToken.issue(
                refreshToken.tokenId(),
                refreshToken.family(),
                userId,
                rawRefreshValue, // valeur brute pour le client
                now,
                now.plus(REFRESH_TOKEN_DURATION)
        );

        return new TokenPair(accessToken, clientRefreshToken);
    }
}
