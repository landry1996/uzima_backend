package com.uzima.application.user;

import com.uzima.application.user.port.in.RegisterUserCommand;
import com.uzima.application.user.port.out.PasswordHasherPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.PhoneNumber;
import com.uzima.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du use case RegisterUser.
 *
 * Pas de Spring. Mocks manuels via Mockito.
 * TimeProvider stubbé avec une valeur fixe.
 */
@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordHasherPort passwordHasher;

    private final TimeProvider clock = () -> Instant.parse("2026-03-12T10:00:00Z");

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordHasher, clock);
    }

    @Nested
    @DisplayName("Cas nominal : inscription réussie")
    class HappyPath {

        @BeforeEach
        void setUp() {
            when(userRepository.existsByPhoneNumber(any(PhoneNumber.class))).thenReturn(false);
            when(passwordHasher.hash(anyString())).thenReturn("$2a$10$hashedPassword");
        }

        @Test
        @DisplayName("retourne l'utilisateur créé avec les bonnes données")
        void returnsCreatedUser() {
            var command = new RegisterUserCommand("+33612345678", "Alice Dupont", "monMotDePasse123");

            User result = useCase.execute(command);

            assertThat(result).isNotNull();
            assertThat(result.phoneNumber().value()).isEqualTo("+33612345678");
            assertThat(result.name().value()).isEqualTo("Alice Dupont");
            assertThat(result.isPremium()).isFalse();
        }

        @Test
        @DisplayName("persiste l'utilisateur dans le repository")
        void savesUserToRepository() {
            var command = new RegisterUserCommand("+33612345678", "Alice Dupont", "monMotDePasse123");

            useCase.execute(command);

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("hache le mot de passe via le port")
        void hashesPasswordViaPort() {
            var command = new RegisterUserCommand("+33612345678", "Alice Dupont", "monMotDePasse123");

            useCase.execute(command);

            verify(passwordHasher, times(1)).hash("monMotDePasse123");
        }

        @Test
        @DisplayName("n'appelle jamais directement Instant.now() (TimeProvider injecté)")
        void usesInjectedClock() {
            // Le test compile et fonctionne sans que le use case appelle Instant.now()
            // Preuve : le TimeProvider retourne une valeur fixe et le test passe
            var command = new RegisterUserCommand("+33612345678", "Alice Dupont", "monMotDePasse123");
            User result = useCase.execute(command);
            assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-03-12T10:00:00Z"));
        }
    }

    @Nested
    @DisplayName("Cas d'erreur : numéro déjà utilisé")
    class PhoneAlreadyUsed {

        @Test
        @DisplayName("lève PhoneNumberAlreadyUsedException si le numéro existe déjà")
        void throwsExceptionForDuplicatePhone() {
            when(userRepository.existsByPhoneNumber(any(PhoneNumber.class))).thenReturn(true);

            var command = new RegisterUserCommand("+33612345678", "Bob Martin", "monMotDePasse123");

            assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RegisterUserUseCase.PhoneNumberAlreadyUsedException.class)
                .hasMessageContaining("+33612345678");
        }

        @Test
        @DisplayName("ne persiste pas l'utilisateur si le numéro existe déjà")
        void doesNotSaveWhenPhoneDuplicated() {
            when(userRepository.existsByPhoneNumber(any(PhoneNumber.class))).thenReturn(true);

            var command = new RegisterUserCommand("+33612345678", "Bob Martin", "monMotDePasse123");

            try {
                useCase.execute(command);
            } catch (RegisterUserUseCase.PhoneNumberAlreadyUsedException ignored) {}

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cas d'erreur : commande invalide")
    class InvalidCommand {

        @Test
        @DisplayName("lève une exception pour un numéro de format invalide")
        void throwsForInvalidPhoneFormat() {
            // La validation du format est déléguée au VO PhoneNumber
            when(userRepository.existsByPhoneNumber(any())).thenReturn(false);

            var command = new RegisterUserCommand("0612345678", "Alice", "monMotDePasse123");

            assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(PhoneNumber.InvalidPhoneNumberException.class);
        }

        @Test
        @DisplayName("rejette un mot de passe trop court (< 8 chars) au niveau commande")
        void throwsForShortPassword() {
            assertThatThrownBy(() -> new RegisterUserCommand("+33612345678", "Alice", "court"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejette une commande nulle")
        void throwsForNullCommand() {
            assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
        }
    }
}
