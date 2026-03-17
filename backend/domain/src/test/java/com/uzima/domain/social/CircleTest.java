package com.uzima.domain.social;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleRule;
import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.social.model.MemberRole;
import com.uzima.domain.social.model.NotificationPolicy;
import com.uzima.domain.social.model.VisibilityLevel;
import com.uzima.domain.social.specification.UserIsCircleAdminSpecification;
import com.uzima.domain.social.specification.UserIsCircleMemberSpecification;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de l'Aggregate Root Circle.
 *
 * Aucune dépendance Spring. TimeProvider fixe pour déterminisme.
 */
@DisplayName("Circle (Aggregate Root)")
class CircleTest {

    private static final Instant NOW   = Instant.parse("2026-03-13T09:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-13T09:05:00Z");

    private final TimeProvider clock      = () -> NOW;
    private final TimeProvider laterClock = () -> LATER;

    private UserId alice;
    private UserId bob;
    private UserId charlie;

    @BeforeEach
    void setUp() {
        alice   = UserId.generate();
        bob     = UserId.generate();
        charlie = UserId.generate();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Test
        @DisplayName("crée un cercle avec le propriétaire automatiquement ajouté en OWNER")
        void createsCircleWithOwnerAsMember() {
            Circle circle = Circle.create("Famille Tchiengue", CircleType.FAMILY, alice, clock);

            assertThat(circle.id()).isNotNull();
            assertThat(circle.name()).isEqualTo("Famille Tchiengue");
            assertThat(circle.type()).isEqualTo(CircleType.FAMILY);
            assertThat(circle.ownerId()).isEqualTo(alice);
            assertThat(circle.createdAt()).isEqualTo(NOW);
            assertThat(circle.memberCount()).isEqualTo(1);
            assertThat(circle.isMember(alice)).isTrue();
            assertThat(circle.isOwner(alice)).isTrue();
        }

        @Test
        @DisplayName("applique les règles par défaut selon le type")
        void appliesDefaultRulesForType() {
            Circle family = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            Circle work   = Circle.create("Boulot",  CircleType.WORK,   alice, clock);

            assertThat(family.rules().notificationPolicy()).isEqualTo(NotificationPolicy.IMMEDIATE);
            assertThat(family.rules().allowsPayments()).isTrue();
            assertThat(work.rules().notificationPolicy()).isEqualTo(NotificationPolicy.URGENT_ONLY);
            assertThat(work.rules().allowsVoiceMessages()).isFalse();
        }

        @Test
        @DisplayName("émet CircleCreatedEvent")
        void emitsCircleCreatedEvent() {
            Circle circle = Circle.create("Amis proches", CircleType.CLOSE_FRIENDS, alice, clock);
            List<DomainEvent> events = circle.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Circle.CircleCreatedEvent.class);

            Circle.CircleCreatedEvent event = (Circle.CircleCreatedEvent) events.get(0);
            assertThat(event.circleId()).isEqualTo(circle.id());
            assertThat(event.ownerId()).isEqualTo(alice);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("lève InvalidCircleNameException si nom vide")
        void throwsWhenNameBlank() {
            assertThatThrownBy(() -> Circle.create("  ", CircleType.FAMILY, alice, clock))
                    .isInstanceOf(Circle.InvalidCircleNameException.class);
        }

        @Test
        @DisplayName("lève InvalidCircleNameException si nom > 100 caractères")
        void throwsWhenNameTooLong() {
            String longName = "A".repeat(101);
            assertThatThrownBy(() -> Circle.create(longName, CircleType.FAMILY, alice, clock))
                    .isInstanceOf(Circle.InvalidCircleNameException.class);
        }

        @Test
        @DisplayName("strip() le nom à la création")
        void stripsNameOnCreation() {
            Circle circle = Circle.create("  Ma Famille  ", CircleType.FAMILY, alice, clock);
            assertThat(circle.name()).isEqualTo("Ma Famille");
        }
    }

    // -------------------------------------------------------------------------
    // addMember()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addMember()")
    class AddMemberTest {

        @Test
        @DisplayName("ajoute un membre avec le bon rôle")
        void addsMemberWithRole() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.pullDomainEvents();

            circle.addMember(bob, MemberRole.MEMBER, laterClock);

            assertThat(circle.isMember(bob)).isTrue();
            assertThat(circle.memberCount()).isEqualTo(2);
            assertThat(circle.membershipOf(bob)).isPresent();
            assertThat(circle.membershipOf(bob).get().role()).isEqualTo(MemberRole.MEMBER);
            assertThat(circle.membershipOf(bob).get().joinedAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("lève DuplicateMemberException si déjà membre")
        void throwsWhenDuplicateMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);

            assertThatThrownBy(() -> circle.addMember(bob, MemberRole.ADMIN, clock))
                    .isInstanceOf(Circle.DuplicateMemberException.class)
                    .hasMessageContaining(bob.toString());
        }

        @Test
        @DisplayName("interdit d'ajouter un membre avec le rôle OWNER")
        void throwsWhenAddingOwnerRole() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);

            assertThatThrownBy(() -> circle.addMember(bob, MemberRole.OWNER, clock))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("émet MemberAddedEvent")
        void emitsMemberAddedEvent() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.pullDomainEvents();

            circle.addMember(bob, MemberRole.ADMIN, laterClock);
            List<DomainEvent> events = circle.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Circle.MemberAddedEvent.class);

            Circle.MemberAddedEvent event = (Circle.MemberAddedEvent) events.get(0);
            assertThat(event.memberId()).isEqualTo(bob);
            assertThat(event.role()).isEqualTo(MemberRole.ADMIN);
            assertThat(event.circleId()).isEqualTo(circle.id());
        }
    }

    // -------------------------------------------------------------------------
    // removeMember()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberTest {

        @Test
        @DisplayName("retire un membre existant")
        void removesMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);
            circle.pullDomainEvents();

            circle.removeMember(bob, laterClock);

            assertThat(circle.isMember(bob)).isFalse();
            assertThat(circle.memberCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("lève OwnerCannotLeaveException si userId est le propriétaire")
        void throwsWhenRemovingOwner() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);

            assertThatThrownBy(() -> circle.removeMember(alice, clock))
                    .isInstanceOf(Circle.OwnerCannotLeaveException.class);
        }

        @Test
        @DisplayName("lève MemberNotFoundException si userId n'est pas membre")
        void throwsWhenNotMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);

            assertThatThrownBy(() -> circle.removeMember(charlie, clock))
                    .isInstanceOf(Circle.MemberNotFoundException.class)
                    .hasMessageContaining(charlie.toString());
        }

        @Test
        @DisplayName("émet MemberRemovedEvent")
        void emitsMemberRemovedEvent() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);
            circle.pullDomainEvents();

            circle.removeMember(bob, laterClock);
            List<DomainEvent> events = circle.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Circle.MemberRemovedEvent.class);

            Circle.MemberRemovedEvent event = (Circle.MemberRemovedEvent) events.get(0);
            assertThat(event.memberId()).isEqualTo(bob);
            assertThat(event.occurredAt()).isEqualTo(LATER);
        }
    }

    // -------------------------------------------------------------------------
    // updateRules()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateRules()")
    class UpdateRulesTest {

        @Test
        @DisplayName("un OWNER peut modifier les règles")
        void ownerCanUpdateRules() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            CircleRule newRules = new CircleRule(
                NotificationPolicy.BLOCKED, VisibilityLevel.PRIVATE, false, false
            );

            circle.updateRules(newRules, alice);

            assertThat(circle.rules()).isEqualTo(newRules);
        }

        @Test
        @DisplayName("un ADMIN peut modifier les règles")
        void adminCanUpdateRules() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.ADMIN, clock);
            CircleRule newRules = new CircleRule(
                NotificationPolicy.DEFERRED, VisibilityLevel.CIRCLE_ONLY, true, false
            );

            circle.updateRules(newRules, bob);

            assertThat(circle.rules()).isEqualTo(newRules);
        }

        @Test
        @DisplayName("lève InsufficientPermissionException si MEMBER essaie de modifier")
        void memberCannotUpdateRules() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);
            CircleRule newRules = CircleRule.defaultForType(CircleType.WORK);

            assertThatThrownBy(() -> circle.updateRules(newRules, bob))
                    .isInstanceOf(Circle.InsufficientPermissionException.class)
                    .hasMessageContaining("ADMIN ou OWNER");
        }

        @Test
        @DisplayName("lève MemberNotFoundException si requesterId n'est pas membre")
        void nonMemberCannotUpdateRules() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);

            assertThatThrownBy(() -> circle.updateRules(CircleRule.defaultForType(CircleType.FAMILY), charlie))
                    .isInstanceOf(Circle.MemberNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // rename()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("rename()")
    class RenameTest {

        @Test
        @DisplayName("un OWNER peut renommer le cercle")
        void ownerCanRename() {
            Circle circle = Circle.create("Ancien nom", CircleType.PROJECT, alice, clock);

            circle.rename("Nouveau nom", alice);

            assertThat(circle.name()).isEqualTo("Nouveau nom");
        }

        @Test
        @DisplayName("lève InsufficientPermissionException si MEMBER essaie de renommer")
        void memberCannotRename() {
            Circle circle = Circle.create("Projet", CircleType.PROJECT, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);

            assertThatThrownBy(() -> circle.rename("Nouveau nom", bob))
                    .isInstanceOf(Circle.InsufficientPermissionException.class);
        }

        @Test
        @DisplayName("lève InvalidCircleNameException si nouveau nom vide")
        void throwsWhenNewNameBlank() {
            Circle circle = Circle.create("Projet", CircleType.PROJECT, alice, clock);

            assertThatThrownBy(() -> circle.rename("", alice))
                    .isInstanceOf(Circle.InvalidCircleNameException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Specifications
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Specifications")
    class SpecificationsTest {

        @Test
        @DisplayName("UserIsCircleMemberSpecification : true pour un membre")
        void memberSpecMatchesMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);

            assertThat(new UserIsCircleMemberSpecification(bob).isSatisfiedBy(circle)).isTrue();
        }

        @Test
        @DisplayName("UserIsCircleMemberSpecification : false pour un non-membre")
        void memberSpecDoesNotMatchNonMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);

            assertThat(new UserIsCircleMemberSpecification(charlie).isSatisfiedBy(circle)).isFalse();
        }

        @Test
        @DisplayName("UserIsCircleAdminSpecification : true pour OWNER")
        void adminSpecMatchesOwner() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);

            assertThat(new UserIsCircleAdminSpecification(alice).isSatisfiedBy(circle)).isTrue();
        }

        @Test
        @DisplayName("UserIsCircleAdminSpecification : true pour ADMIN")
        void adminSpecMatchesAdmin() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.ADMIN, clock);

            assertThat(new UserIsCircleAdminSpecification(bob).isSatisfiedBy(circle)).isTrue();
        }

        @Test
        @DisplayName("UserIsCircleAdminSpecification : false pour MEMBER")
        void adminSpecDoesNotMatchMember() {
            Circle circle = Circle.create("Famille", CircleType.FAMILY, alice, clock);
            circle.addMember(bob, MemberRole.MEMBER, clock);

            assertThat(new UserIsCircleAdminSpecification(bob).isSatisfiedBy(circle)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // reconstitute()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("reconstitute()")
    class ReconstitueTest {

        @Test
        @DisplayName("reconstitue un cercle avec ses membres sans émettre d'événements")
        void reconstitutesWithoutEvents() {
            var membership = new com.uzima.domain.social.model.CircleMembership(
                alice, MemberRole.OWNER, NOW
            );

            Circle circle = Circle.reconstitute(
                com.uzima.domain.social.model.CircleId.generate(),
                "Famille reconstituée",
                CircleType.FAMILY,
                alice,
                CircleRule.defaultForType(CircleType.FAMILY),
                NOW,
                List.of(membership)
            );

            assertThat(circle.memberCount()).isEqualTo(1);
            assertThat(circle.pullDomainEvents()).isEmpty();
        }
    }
}
