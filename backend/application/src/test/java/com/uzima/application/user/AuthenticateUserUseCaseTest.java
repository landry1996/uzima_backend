package com.uzima.application.user;

import com.uzima.application.security.BruteForceProtectionService;
import com.uzima.application.security.LoginAttemptRepositoryPort;
import com.uzima.application.user.port.in.AuthenticateUserCommand;
import com.uzima.application.user.port.out.PasswordHasherPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.security.AccountLockoutPolicy;
import com.uzima.domain.security.LoginAttempt;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du use case AuthenticateUserUseCase.
 *
 * Vérifie notamment la protection anti brute force :
 * - AccountTemporarilyLockedException.identifier() : retourne le numéro verrouillé
 * - AccountTemporarilyLockedException.lockDuration() : retourne la durée de blocage
 */
@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private static final String PHONE = "+237612345678";
    private static final String PASSWORD = "monMotDePasse123";
    private static final String HASH = "$2a$10$hashedPassword";

    private final TimeProvider clock = () -> NOW;

    @Mock private UserRepositoryPort userRepository;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private LoginAttemptRepositoryPort loginAttemptRepository;

    private AuthenticateUserUseCase useCase;
    private BruteForceProtectionService bruteForceService;

    @BeforeEach
    void setUp() {
        // Politique configurable avec seuil bas (3 échecs) pour faciliter les tests
        AccountLockoutPolicy policy = new AccountLockoutPolicy(
                3, 6, 10,
                Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(24)
        );
        bruteForceService = new BruteForceProtectionService(policy, loginAttemptRepository, clock);
        useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, bruteForceService, clock);
    }

    // -------------------------------------------------------------------------
    // Cas nominal
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Authentification réussie")
    class HappyPath {

        @Test
        @DisplayName("retourne l'utilisateur si les identifiants sont corrects")
        void returnsUserOnSuccess() {
            User user = buildUser();
            when(loginAttemptRepository.findRecentAttempts(eq(PHONE), any(), any()))
                    .thenReturn(List.of());
            when(userRepository.findByPhoneNumber(any(PhoneNumber.class)))
                    .thenReturn(Optional.of(user));
            when(passwordHasher.matches(PASSWORD, HASH)).thenReturn(true);

            User result = useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD));

            assertThat(result).isEqualTo(user);
            verify(loginAttemptRepository).save(argThat(a -> a.identifier().equals(PHONE) && !a.isFailed()));
        }
    }

    // -------------------------------------------------------------------------
    // Protection brute force — identifier() et lockDuration()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Protection brute force — AccountTemporarilyLockedException")
    class BruteForceProtection {

        @Test
        @DisplayName("lève AccountTemporarilyLockedException si le compte est verrouillé")
        void throwsWhenAccountLocked() {
            // Simule 3 échecs dans la fenêtre de 15 min → seuil soft atteint
            List<LoginAttempt> failedAttempts = List.of(
                    new LoginAttempt(PHONE, NOW.minusSeconds(60), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(120), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(180), false)
            );
            when(loginAttemptRepository.findRecentAttempts(eq(PHONE), any(), any()))
                    .thenReturn(failedAttempts);

            assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD)))
                    .isInstanceOf(BruteForceProtectionService.AccountTemporarilyLockedException.class);
        }

        @Test
        @DisplayName("identifier() retourne le numéro de téléphone verrouillé")
        void identifierReturnsLockedPhoneNumber() {
            // Simule le compte verrouillé
            List<LoginAttempt> failedAttempts = List.of(
                    new LoginAttempt(PHONE, NOW.minusSeconds(60), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(120), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(180), false)
            );
            when(loginAttemptRepository.findRecentAttempts(eq(PHONE), any(), any()))
                    .thenReturn(failedAttempts);

            // Attrape l'exception et vérifie identifier()
            try {
                useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD));
                fail("AccountTemporarilyLockedException attendue");
            } catch (BruteForceProtectionService.AccountTemporarilyLockedException ex) {
                // identifier() : retourne le numéro de téléphone qui a déclenché le verrouillage
                assertThat(ex.identifier()).isEqualTo(PHONE);
            }
        }

        @Test
        @DisplayName("lockDuration() retourne la durée de verrouillage (5 min pour soft lock)")
        void lockDurationReturnsSoftLockDuration() {
            List<LoginAttempt> failedAttempts = List.of(
                    new LoginAttempt(PHONE, NOW.minusSeconds(60), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(120), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(180), false)
            );
            when(loginAttemptRepository.findRecentAttempts(eq(PHONE), any(), any()))
                    .thenReturn(failedAttempts);

            try {
                useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD));
                fail("AccountTemporarilyLockedException attendue");
            } catch (BruteForceProtectionService.AccountTemporarilyLockedException ex) {
                // lockDuration() : retourne la durée configurée pour le soft lock (5 min)
                assertThat(ex.lockDuration()).isEqualTo(Duration.ofMinutes(5));
            }
        }

        @Test
        @DisplayName("identifier() et lockDuration() accessibles ensemble pour affichage UX")
        void identifierAndLockDurationUsableForUxDisplay() {
            // Simule le medium lock (6 échecs en 1h)
            List<LoginAttempt> failedAttempts = List.of(
                    new LoginAttempt(PHONE, NOW.minusSeconds(100), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(200), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(300), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(400), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(500), false),
                    new LoginAttempt(PHONE, NOW.minusSeconds(600), false)
            );
            when(loginAttemptRepository.findRecentAttempts(eq(PHONE), any(), any()))
                    .thenReturn(failedAttempts);

            try {
                useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD));
                fail("AccountTemporarilyLockedException attendue");
            } catch (BruteForceProtectionService.AccountTemporarilyLockedException ex) {
                // identifier() + lockDuration() : utilisés ensemble pour afficher un message UX
                String uxMessage = "Le compte " + ex.identifier()
                        + " est verrouillé pour " + ex.lockDuration().toMinutes() + " minutes";

                assertThat(ex.identifier()).isEqualTo(PHONE);
                assertThat(ex.lockDuration()).isEqualTo(Duration.ofMinutes(30)); // medium lock
                assertThat(uxMessage).contains(PHONE).contains("30 minutes");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cas d'erreur
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Identifiants invalides")
    class InvalidCredentials {

        @BeforeEach
        void setUp() {
            when(loginAttemptRepository.findRecentAttempts(eq(PHONE), any(), any()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("lève AuthenticationFailedException si l'utilisateur n'existe pas")
        void throwsWhenUserNotFound() {
            when(userRepository.findByPhoneNumber(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD)))
                    .isInstanceOf(AuthenticateUserUseCase.AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("lève AuthenticationFailedException si le mot de passe est incorrect")
        void throwsWhenPasswordWrong() {
            User user = buildUser();
            when(userRepository.findByPhoneNumber(any())).thenReturn(Optional.of(user));
            when(passwordHasher.matches(PASSWORD, HASH)).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD)))
                    .isInstanceOf(AuthenticateUserUseCase.AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("enregistre la tentative même en cas d'échec")
        void recordsAttemptOnFailure() {
            when(userRepository.findByPhoneNumber(any())).thenReturn(Optional.empty());

            try { useCase.execute(new AuthenticateUserCommand(PHONE, PASSWORD)); }
            catch (Exception ignored) {}

            verify(loginAttemptRepository).save(argThat(a -> a.isFailed()));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser() {
        return User.reconstitute(
                UserId.generate(),
                PhoneNumber.of(PHONE),
                CountryCode.of("CM"),
                new FirstName("Alice"),
                new LastName("Dupont"),
                HASH,
                NOW,
                PresenceStatus.AVAILABLE,
                false,
                null
        );
    }
}
