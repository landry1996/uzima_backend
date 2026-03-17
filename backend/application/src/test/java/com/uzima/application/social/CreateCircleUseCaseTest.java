package com.uzima.application.social;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.social.port.in.CreateCircleCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de CreateCircleUseCase.
 *
 * Pas de Spring. Mock Mockito pour CircleRepositoryPort.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCircleUseCase")
class CreateCircleUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-13T09:00:00Z");
    private final TimeProvider clock = () -> NOW;

    @Mock
    private CircleRepositoryPort circleRepository;

    private CreateCircleUseCase useCase;
    private UserId alice;

    @BeforeEach
    void setUp() {
        useCase = new CreateCircleUseCase(circleRepository, clock);
        alice   = UserId.generate();
    }

    // -------------------------------------------------------------------------
    // Nominal
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Nominal")
    class NominalTest {

        @Test
        @DisplayName("crée un cercle et retourne son identifiant")
        void createsCircleAndReturnsId() {
            CreateCircleCommand command = new CreateCircleCommand(alice, "Famille Tchiengue", CircleType.FAMILY);

            CircleId id = useCase.execute(command);

            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("persiste le cercle avec les bonnes données")
        void savesCircleWithCorrectData() {
            CreateCircleCommand command = new CreateCircleCommand(alice, "Boulot", CircleType.WORK);

            useCase.execute(command);

            ArgumentCaptor<Circle> captor = ArgumentCaptor.forClass(Circle.class);
            verify(circleRepository).save(captor.capture());

            Circle saved = captor.getValue();
            assertThat(saved.name()).isEqualTo("Boulot");
            assertThat(saved.type()).isEqualTo(CircleType.WORK);
            assertThat(saved.ownerId()).isEqualTo(alice);
            assertThat(saved.isMember(alice)).isTrue();
            assertThat(saved.isOwner(alice)).isTrue();
        }

        @Test
        @DisplayName("applique les règles par défaut selon le type")
        void appliesDefaultRulesForType() {
            CreateCircleCommand family  = new CreateCircleCommand(alice, "Famille",   CircleType.FAMILY);
            CreateCircleCommand community = new CreateCircleCommand(alice, "Quartier", CircleType.COMMUNITY);

            useCase.execute(family);
            useCase.execute(community);

            ArgumentCaptor<Circle> captor = ArgumentCaptor.forClass(Circle.class);
            verify(circleRepository, times(2)).save(captor.capture());

            Circle familyCircle    = captor.getAllValues().get(0);
            Circle communityCircle = captor.getAllValues().get(1);

            assertThat(familyCircle.rules().allowsPayments()).isTrue();
            assertThat(communityCircle.rules().allowsPayments()).isFalse();
        }

        @Test
        @DisplayName("save() est appelé exactement une fois")
        void saveCalledOnce() {
            useCase.execute(new CreateCircleCommand(alice, "Projet X", CircleType.PROJECT));

            verify(circleRepository, times(1)).save(any(Circle.class));
        }
    }

    // -------------------------------------------------------------------------
    // Cas d'erreur
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Erreurs domaine")
    class ErrorTest {

        @Test
        @DisplayName("lève InvalidCircleNameException si nom vide")
        void throwsWhenNameBlank() {
            CreateCircleCommand command = new CreateCircleCommand(alice, "   ", CircleType.FAMILY);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(Circle.InvalidCircleNameException.class);

            verify(circleRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève InvalidCircleNameException si nom > 100 caractères")
        void throwsWhenNameTooLong() {
            CreateCircleCommand command = new CreateCircleCommand(alice, "A".repeat(101), CircleType.FAMILY);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(Circle.InvalidCircleNameException.class);

            verify(circleRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève NullPointerException si commande nulle")
        void throwsWhenCommandNull() {
            assertThatThrownBy(() -> useCase.execute(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
