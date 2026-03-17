package com.uzima.application.social;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.social.port.in.AddMemberCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.social.model.MemberRole;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AddMemberToCircleUseCase.
 *
 * Pas de Spring. Mock Mockito pour CircleRepositoryPort.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddMemberToCircleUseCase")
class AddMemberToCircleUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-13T09:00:00Z");
    private final TimeProvider clock = () -> NOW;

    @Mock
    private CircleRepositoryPort circleRepository;

    private AddMemberToCircleUseCase useCase;

    private UserId alice;
    private UserId bob;
    private UserId charlie;

    @BeforeEach
    void setUp() {
        useCase  = new AddMemberToCircleUseCase(circleRepository, clock);
        alice    = UserId.generate();
        bob      = UserId.generate();
        charlie  = UserId.generate();
    }

    // -------------------------------------------------------------------------
    // Nominal
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Nominal")
    class NominalTest {

        @Test
        @DisplayName("un OWNER peut ajouter un membre MEMBER")
        void ownerCanAddMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            AddMemberCommand command = new AddMemberCommand(circle.id(), alice, bob, MemberRole.MEMBER);
            useCase.execute(command);

            ArgumentCaptor<Circle> captor = ArgumentCaptor.forClass(Circle.class);
            verify(circleRepository).save(captor.capture());

            Circle saved = captor.getValue();
            assertThat(saved.isMember(bob)).isTrue();
            assertThat(saved.membershipOf(bob).get().role()).isEqualTo(MemberRole.MEMBER);
        }

        @Test
        @DisplayName("un ADMIN peut ajouter un membre")
        void adminCanAddMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.ADMIN, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            AddMemberCommand command = new AddMemberCommand(circle.id(), bob, charlie, MemberRole.MEMBER);
            useCase.execute(command);

            verify(circleRepository).save(argThat(c -> c.isMember(charlie)));
        }

        @Test
        @DisplayName("save() est appelé exactement une fois après ajout réussi")
        void saveCalledOnce() {
            Circle circle = Circle.create("Projet", CircleType.PROJECT, alice, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            useCase.execute(new AddMemberCommand(circle.id(), alice, bob, MemberRole.GUEST));

            verify(circleRepository, times(1)).save(any(Circle.class));
        }
    }

    // -------------------------------------------------------------------------
    // Erreurs
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Erreurs")
    class ErrorTest {

        @Test
        @DisplayName("lève ResourceNotFoundException si cercle inexistant")
        void throwsWhenCircleNotFound() {
            CircleId unknownId = CircleId.generate();
            when(circleRepository.findById(unknownId)).thenReturn(Optional.empty());

            AddMemberCommand command = new AddMemberCommand(unknownId, alice, bob, MemberRole.MEMBER);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());

            verify(circleRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève CircleAccessDeniedException si le demandeur est MEMBER")
        void throwsWhenRequesterIsMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            AddMemberCommand command = new AddMemberCommand(circle.id(), bob, charlie, MemberRole.MEMBER);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(AddMemberToCircleUseCase.CircleAccessDeniedException.class)
                    .hasMessageContaining("ADMIN ou OWNER");

            verify(circleRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève CircleAccessDeniedException si le demandeur n'est pas membre")
        void throwsWhenRequesterNotMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            AddMemberCommand command = new AddMemberCommand(circle.id(), charlie, bob, MemberRole.MEMBER);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(AddMemberToCircleUseCase.CircleAccessDeniedException.class);

            verify(circleRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève DuplicateMemberException si le membre est déjà dans le cercle")
        void throwsWhenDuplicateMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            AddMemberCommand command = new AddMemberCommand(circle.id(), alice, bob, MemberRole.ADMIN);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(Circle.DuplicateMemberException.class);

            verify(circleRepository, never()).save(any());
        }

        @Test
        @DisplayName("interdit d'ajouter un membre avec le rôle OWNER")
        void throwsWhenRoleIsOwner() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            when(circleRepository.findById(circle.id())).thenReturn(Optional.of(circle));

            AddMemberCommand command = new AddMemberCommand(circle.id(), alice, bob, MemberRole.OWNER);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(circleRepository, never()).save(any());
        }
    }
}
