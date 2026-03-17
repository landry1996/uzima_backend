package com.uzima.security.token.usecase;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.AccessToken;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.model.TokenFamily;
import com.uzima.security.token.model.TokenId;
import com.uzima.security.token.model.TokenPair;
import com.uzima.security.token.port.AccessTokenGeneratorPort;
import com.uzima.security.token.port.RefreshTokenHasherPort;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshAccessTokenUseCaseTest {

    private AccessTokenGeneratorPort accessTokenGenerator;
    private RefreshTokenHasherPort refreshTokenHasher;
    private RefreshTokenRepositoryPort refreshTokenRepository;
    private TimeProvider clock;
    private RefreshAccessTokenUseCase useCase;

    private final Instant NOW = Instant.parse("2025-06-01T10:00:00Z");
    private final UserId USER_ID = UserId.generate();
    private final TokenFamily FAMILY = TokenFamily.generate();
    private final String RAW_TOKEN = "raw-refresh-token";
    private final String HASHED_TOKEN = "hashed-refresh-token";

    @BeforeEach
    void setUp() {
        accessTokenGenerator = mock(AccessTokenGeneratorPort.class);
        refreshTokenHasher = mock(RefreshTokenHasherPort.class);
        refreshTokenRepository = mock(RefreshTokenRepositoryPort.class);
        clock = mock(TimeProvider.class);
        when(clock.now()).thenReturn(NOW);

        useCase = new RefreshAccessTokenUseCase(
                accessTokenGenerator, refreshTokenHasher, refreshTokenRepository, clock
        );
    }

    @Test
    void execute_validToken_returnsNewTokenPair() {
        // Given
        var existingRefresh = validRefreshToken();
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(HASHED_TOKEN);
        when(refreshTokenRepository.findByHashedValue(HASHED_TOKEN)).thenReturn(Optional.of(existingRefresh));
        when(refreshTokenHasher.generateRawValue()).thenReturn("new-raw-value");
        when(refreshTokenHasher.hash("new-raw-value")).thenReturn("new-hashed-value");
        when(accessTokenGenerator.generate(USER_ID)).thenReturn(mockAccessToken());

        // When
        TokenPair pair = useCase.execute(RAW_TOKEN);

        // Then
        assertThat(pair.accessToken()).isNotNull();
        assertThat(pair.refreshToken()).isNotNull();
        assertThat(pair.refreshToken().family()).isEqualTo(FAMILY); // même famille
        verify(refreshTokenRepository).update(existingRefresh); // ancien révoqué
        verify(refreshTokenRepository).save(any()); // nouveau sauvegardé
    }

    @Test
    void execute_revokedToken_revokesEntireFamilyAndThrows() {
        // Given — token déjà révoqué (replay attack)
        var revokedRefresh = RefreshToken.reconstitute(
                TokenId.generate(), FAMILY, USER_ID, HASHED_TOKEN,
                NOW.minusSeconds(100), NOW.plusSeconds(3600),
                true, NOW.minusSeconds(50)
        );
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(HASHED_TOKEN);
        when(refreshTokenRepository.findByHashedValue(HASHED_TOKEN)).thenReturn(Optional.of(revokedRefresh));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN))
                .isInstanceOf(RefreshAccessTokenUseCase.TokenRefreshException.class)
                .hasMessageContaining("compromis");

        verify(refreshTokenRepository).revokeFamily(FAMILY); // toute la famille révoquée
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void execute_expiredToken_throws() {
        // Given — token expiré (pas révoqué)
        var expiredRefresh = RefreshToken.reconstitute(
                TokenId.generate(), FAMILY, USER_ID, HASHED_TOKEN,
                NOW.minusSeconds(7200), NOW.minusSeconds(3600), // expiré il y a 1h
                false, null
        );
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(HASHED_TOKEN);
        when(refreshTokenRepository.findByHashedValue(HASHED_TOKEN)).thenReturn(Optional.of(expiredRefresh));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN))
                .isInstanceOf(RefreshAccessTokenUseCase.TokenRefreshException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    void execute_unknownToken_throws() {
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(HASHED_TOKEN);
        when(refreshTokenRepository.findByHashedValue(HASHED_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN))
                .isInstanceOf(RefreshAccessTokenUseCase.TokenRefreshException.class)
                .hasMessageContaining("inconnu");
    }

    private RefreshToken validRefreshToken() {
        return RefreshToken.reconstitute(
                TokenId.generate(), FAMILY, USER_ID, HASHED_TOKEN,
                NOW.minusSeconds(100), NOW.plusSeconds(3600),
                false, null
        );
    }

    private AccessToken mockAccessToken() {
        return new AccessToken(TokenId.generate(), USER_ID, "jwt.token.value", NOW, NOW.plusSeconds(900));
    }
}
