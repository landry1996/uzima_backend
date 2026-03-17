package com.uzima.infrastructure.security;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.AccessToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de JwtAccessTokenGenerator.
 *
 * Vérifie les deux constructeurs :
 * - JwtAccessTokenGenerator(String, Duration) : constructeur complet (utilisé en prod)
 * - JwtAccessTokenGenerator(String)            : constructeur 1-arg (durée par défaut 15 min)
 */
class JwtAccessTokenGeneratorTest {

    // Clé de 32 caractères minimum (requis par HS256)
    private static final String SECRET = "uzima-test-secret-key-32-chars!!";
    private static final UserId USER_ID = UserId.generate();

    @Nested
    @DisplayName("Constructeur 2-args (secret + durée explicite)")
    class TwoArgConstructor {

        @Test
        @DisplayName("génère un access token avec la durée configurée")
        void generatesTokenWithConfiguredExpiry() {
            Duration validity = Duration.ofMinutes(30);
            JwtAccessTokenGenerator generator = new JwtAccessTokenGenerator(SECRET, validity);

            AccessToken token = generator.generate(USER_ID);

            assertThat(token).isNotNull();
            assertThat(token.userId()).isEqualTo(USER_ID);
            assertThat(token.rawValue()).isNotBlank();
            // Vérifie que la durée de vie est approximativement correcte (±5s)
            long actualDuration = token.expiresAt().toEpochMilli() - token.issuedAt().toEpochMilli();
            assertThat(actualDuration).isBetween(
                    validity.toMillis() - 5_000,
                    validity.toMillis() + 5_000
            );
        }

        @Test
        @DisplayName("lève IllegalArgumentException si la clé est trop courte (< 32 chars)")
        void throwsWhenSecretTooShort() {
            assertThatThrownBy(() -> new JwtAccessTokenGenerator("short-key", Duration.ofMinutes(15)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32");
        }
    }

    @Nested
    @DisplayName("Constructeur 1-arg (durée par défaut = 15 min)")
    class OneArgConstructor {

        @Test
        @DisplayName("génère un access token avec la durée par défaut (15 min)")
        void generatesTokenWithDefaultExpiry() {
            // Constructeur 1-arg : JwtAccessTokenGenerator(String secretKeyString)
            // Délègue au constructeur 2-args avec DEFAULT_VALIDITY = 15 minutes
            JwtAccessTokenGenerator generator = new JwtAccessTokenGenerator(SECRET);

            AccessToken token = generator.generate(USER_ID);

            assertThat(token).isNotNull();
            assertThat(token.userId()).isEqualTo(USER_ID);
            assertThat(token.rawValue()).isNotBlank();
            // Durée par défaut : 15 minutes (900 000 ms), tolérance ±5s
            long actualDuration = token.expiresAt().toEpochMilli() - token.issuedAt().toEpochMilli();
            assertThat(actualDuration).isBetween(
                    Duration.ofMinutes(15).toMillis() - 5_000,
                    Duration.ofMinutes(15).toMillis() + 5_000
            );
        }

        @Test
        @DisplayName("le token généré via 1-arg a le même userId que demandé")
        void tokenContainsCorrectUserId() {
            JwtAccessTokenGenerator generator = new JwtAccessTokenGenerator(SECRET);

            AccessToken token = generator.generate(USER_ID);

            assertThat(token.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("le tokenId est non nul et unique à chaque appel")
        void tokenIdIsUniqueOnEachCall() {
            JwtAccessTokenGenerator generator = new JwtAccessTokenGenerator(SECRET);

            AccessToken token1 = generator.generate(USER_ID);
            AccessToken token2 = generator.generate(USER_ID);

            assertThat(token1.tokenId()).isNotEqualTo(token2.tokenId());
        }
    }
}
