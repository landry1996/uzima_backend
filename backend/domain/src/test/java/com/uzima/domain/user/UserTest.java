package com.uzima.domain.user;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du domaine User.
 *
 * Ces tests s'exécutent sans Spring, sans base de données, sans aucun framework.
 * Le TimeProvider est un stub déterministe.
 */
class UserTest {

    // Stub déterministe du TimeProvider (pas de Instant.now() dans les tests)
    private static final Instant FIXED_TIME = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> FIXED_TIME;

    private PhoneNumber validPhone;
    private CountryCode validCountry;
    private FirstName validFirstName;
    private LastName validLastName;
    private String validHash;

    @BeforeEach
    void setUp() {
        validPhone = PhoneNumber.of("+33612345678");
        validCountry = CountryCode.of("FR");
        validFirstName = FirstName.of("Alice");
        validLastName = LastName.of("Dupont");
        validHash = "$2a$10$hashedPasswordExample";
    }

    // -------------------------------------------------------------------------
    // Helper pour créer un utilisateur valide
    // -------------------------------------------------------------------------

    private User registerValid() {
        return User.register(validPhone, validCountry, validFirstName, validLastName, validHash, clock);
    }

    @Nested
    @DisplayName("PhoneNumber Value Object")
    class PhoneNumberTest {

        @Test
        @DisplayName("accepte un numéro E.164 valide (France)")
        void acceptsValidFrenchNumber() {
            assertThatNoException().isThrownBy(() -> PhoneNumber.of("+33612345678"));
        }

        @Test
        @DisplayName("accepte un numéro E.164 valide (Cameroun)")
        void acceptsValidCameroonNumber() {
            assertThatNoException().isThrownBy(() -> PhoneNumber.of("+237612345678"));
        }

        @Test
        @DisplayName("rejette un numéro sans préfixe +")
        void rejectsNumberWithoutPlus() {
            assertThatThrownBy(() -> PhoneNumber.of("0612345678"))
                .isInstanceOf(PhoneNumber.InvalidPhoneNumberException.class);
        }

        @Test
        @DisplayName("rejette un numéro nul")
        void rejectsNull() {
            assertThatThrownBy(() -> PhoneNumber.of(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejette un numéro trop court")
        void rejectsTooShort() {
            assertThatThrownBy(() -> PhoneNumber.of("+123"))
                .isInstanceOf(PhoneNumber.InvalidPhoneNumberException.class);
        }

        @Test
        @DisplayName("deux PhoneNumber avec la même valeur sont égaux (record)")
        void equalityByValue() {
            var p1 = PhoneNumber.of("+33612345678");
            var p2 = PhoneNumber.of("+33612345678");
            assertThat(p1).isEqualTo(p2);
        }
    }

    @Nested
    @DisplayName("CountryCode Value Object")
    class CountryCodeTest {

        @Test
        @DisplayName("accepte un code pays valide (FR)")
        void acceptsFrance() {
            assertThatNoException().isThrownBy(() -> CountryCode.of("FR"));
        }

        @Test
        @DisplayName("accepte un code pays valide (CM - Cameroun)")
        void acceptsCameroon() {
            assertThatNoException().isThrownBy(() -> CountryCode.of("CM"));
        }

        @Test
        @DisplayName("normalise en majuscules")
        void normalizesToUppercase() {
            var code = CountryCode.of("fr");
            assertThat(code.value()).isEqualTo("FR");
        }

        @Test
        @DisplayName("rejette un code inconnu")
        void rejectsUnknownCode() {
            assertThatThrownBy(() -> CountryCode.of("XX"))
                .isInstanceOf(CountryCode.InvalidCountryCodeException.class);
        }

        @Test
        @DisplayName("rejette un code trop long")
        void rejectsTooLong() {
            assertThatThrownBy(() -> CountryCode.of("FRA"))
                .isInstanceOf(CountryCode.InvalidCountryCodeException.class);
        }
    }

    @Nested
    @DisplayName("FirstName Value Object")
    class FirstNameTest {

        @Test
        @DisplayName("accepte un prénom valide")
        void acceptsValid() {
            assertThatNoException().isThrownBy(() -> FirstName.of("Pierre"));
        }

        @Test
        @DisplayName("rejette un prénom vide")
        void rejectsBlank() {
            assertThatThrownBy(() -> FirstName.of("   "))
                .isInstanceOf(FirstName.InvalidFirstNameException.class);
        }

        @Test
        @DisplayName("trim les espaces")
        void trimsWhitespace() {
            var name = FirstName.of("  Alice  ");
            assertThat(name.value()).isEqualTo("Alice");
        }
    }

    @Nested
    @DisplayName("LastName Value Object")
    class LastNameTest {

        @Test
        @DisplayName("accepte un nom valide")
        void acceptsValid() {
            assertThatNoException().isThrownBy(() -> LastName.of("Dupont"));
        }

        @Test
        @DisplayName("rejette un nom vide")
        void rejectsBlank() {
            assertThatThrownBy(() -> LastName.of("   "))
                .isInstanceOf(LastName.InvalidLastNameException.class);
        }

        @Test
        @DisplayName("trim les espaces")
        void trimsWhitespace() {
            var name = LastName.of("  Martin  ");
            assertThat(name.value()).isEqualTo("Martin");
        }
    }

    @Nested
    @DisplayName("User.register() factory method")
    class RegisterTest {

        @Test
        @DisplayName("crée un utilisateur avec un état initial valide")
        void createsUserWithValidInitialState() {
            User user = registerValid();

            assertThat(user.id()).isNotNull();
            assertThat(user.phoneNumber()).isEqualTo(validPhone);
            assertThat(user.country()).isEqualTo(validCountry);
            assertThat(user.firstName()).isEqualTo(validFirstName);
            assertThat(user.lastName()).isEqualTo(validLastName);
            assertThat(user.fullName()).isEqualTo("Alice Dupont");
            assertThat(user.presenceStatus()).isEqualTo(PresenceStatus.AVAILABLE);
            assertThat(user.isPremium()).isFalse();
            assertThat(user.createdAt()).isEqualTo(FIXED_TIME);
            assertThat(user.avatarUrl()).isEmpty();
        }

        @Test
        @DisplayName("émet un événement UserRegistered")
        void emitsDomainEvent() {
            User user = registerValid();
            var events = user.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(User.UserRegisteredEvent.class);
        }

        @Test
        @DisplayName("l'événement UserRegistered contient le pays")
        void registeredEventContainsCountry() {
            User user = registerValid();
            var event = (User.UserRegisteredEvent) user.pullDomainEvents().get(0);

            assertThat(event.country()).isEqualTo(validCountry);
        }

        @Test
        @DisplayName("les événements sont vidés après pullDomainEvents()")
        void eventsAreClearedAfterPull() {
            User user = registerValid();
            user.pullDomainEvents(); // premier pull
            assertThat(user.pullDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("rejette un passwordHash nul")
        void rejectsNullPasswordHash() {
            assertThatThrownBy(() ->
                User.register(validPhone, validCountry, validFirstName, validLastName, null, clock))
                .isInstanceOf(User.InvalidPasswordHashException.class);
        }

        @Test
        @DisplayName("rejette un passwordHash vide")
        void rejectsBlankPasswordHash() {
            assertThatThrownBy(() ->
                User.register(validPhone, validCountry, validFirstName, validLastName, "", clock))
                .isInstanceOf(User.InvalidPasswordHashException.class);
        }

        @Test
        @DisplayName("rejette un TimeProvider nul")
        void rejectsNullClock() {
            assertThatThrownBy(() ->
                User.register(validPhone, validCountry, validFirstName, validLastName, validHash, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("User.updatePresenceStatus()")
    class PresenceStatusTest {

        @Test
        @DisplayName("met à jour l'état de présence")
        void updatesPresenceStatus() {
            User user = registerValid();
            user.pullDomainEvents();

            user.updatePresenceStatus(PresenceStatus.FOCUSED, clock);

            assertThat(user.presenceStatus()).isEqualTo(PresenceStatus.FOCUSED);
        }

        @Test
        @DisplayName("rejette un état nul")
        void rejectsNullStatus() {
            User user = registerValid();
            assertThatThrownBy(() -> user.updatePresenceStatus(null, clock))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("SILENCE n'autorise pas les appels")
        void silenceDoesNotAllowCalls() {
            assertThat(PresenceStatus.SILENCE.allowsPhoneCalls()).isFalse();
        }

        @Test
        @DisplayName("WELLNESS bloque toutes les notifications")
        void wellnessBlocksAllNotifications() {
            assertThat(PresenceStatus.WELLNESS.notificationPolicy())
                .isEqualTo(PresenceStatus.NotificationPolicy.BLOCKED);
        }
    }

    @Nested
    @DisplayName("User.upgradeToPremium()")
    class PremiumTest {

        @Test
        @DisplayName("upgrade vers premium")
        void upgradesToPremium() {
            User user = registerValid();
            user.upgradeToPremium(clock);
            assertThat(user.isPremium()).isTrue();
        }

        @Test
        @DisplayName("idempotent : double upgrade ne génère qu'un seul événement")
        void idempotentUpgrade() {
            User user = registerValid();
            user.pullDomainEvents();

            user.upgradeToPremium(clock);
            user.upgradeToPremium(clock);

            var events = user.pullDomainEvents();
            long premiumEvents = events.stream()
                .filter(e -> e instanceof User.UserUpgradedToPremiumEvent)
                .count();
            assertThat(premiumEvents).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Égalité par identité (UserId)")
    class EqualityTest {

        @Test
        @DisplayName("deux utilisateurs différents ne sont jamais égaux même avec même téléphone")
        void differentUsersAreNotEqual() {
            User u1 = registerValid();
            User u2 = registerValid();
            assertThat(u1).isNotEqualTo(u2);
        }

        @Test
        @DisplayName("la même référence est égale à elle-même")
        void sameReferenceIsEqual() {
            User user = registerValid();
            assertThat(user).isEqualTo(user);
        }
    }
}
