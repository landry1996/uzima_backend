package com.uzima.domain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de la politique de verrouillage.
 * Aucun Spring, aucune DB. TimeProvider stubbé.
 */
class AccountLockoutPolicyTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private static final String IDENTIFIER = "+33612345678";

    // Politique avec seuils réduits pour faciliter les tests
    private AccountLockoutPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AccountLockoutPolicy(
                3, 5, 8,                              // seuils (doux/moyen/dur)
                Duration.ofMinutes(5),                 // fenêtre doux
                Duration.ofMinutes(30),                // fenêtre moyen
                Duration.ofHours(1),                   // fenêtre dur
                Duration.ofMinutes(1),                 // lock doux
                Duration.ofMinutes(5),                 // lock moyen
                Duration.ofMinutes(30)                 // lock dur
        );
    }

    @Nested
    @DisplayName("Compte non verrouillé")
    class NotLocked {

        @Test
        @DisplayName("autorise sans tentative")
        void allowsWithNoAttempts() {
            LockoutDecision decision = policy.evaluate(List.of(), NOW);
            assertThat(decision.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("autorise avec moins de 3 échecs")
        void allowsWithFewFailures() {
            List<LoginAttempt> attempts = buildFailures(2, NOW.minusSeconds(60));
            LockoutDecision decision = policy.evaluate(attempts, NOW);
            assertThat(decision.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("les succès ne comptent pas comme échecs")
        void successDoesNotCount() {
            List<LoginAttempt> attempts = new ArrayList<>();
            attempts.addAll(buildFailures(2, NOW.minusSeconds(60)));
            attempts.add(new LoginAttempt(IDENTIFIER, NOW.minusSeconds(30), true)); // succès
            LockoutDecision decision = policy.evaluate(attempts, NOW);
            assertThat(decision.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("les échecs anciens (hors fenêtre) ne comptent pas")
        void oldFailuresNotCounted() {
            // 3 échecs il y a 10 min — hors fenêtre de 5 min
            List<LoginAttempt> attempts = buildFailures(3, NOW.minusSeconds(610));
            LockoutDecision decision = policy.evaluate(attempts, NOW);
            assertThat(decision.isAllowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Verrouillage doux")
    class SoftLock {

        @Test
        @DisplayName("verrouille après 3 échecs dans les 5 dernières minutes")
        void locksAfterSoftThreshold() {
            List<LoginAttempt> attempts = buildFailures(3, NOW.minusSeconds(60));
            LockoutDecision decision = policy.evaluate(attempts, NOW);
            assertThat(decision.isLocked()).isTrue();
            assertThat(decision.remainingLockDuration()).hasValue(Duration.ofMinutes(1));
        }
    }

    @Nested
    @DisplayName("Verrouillage moyen")
    class MediumLock {

        @Test
        @DisplayName("verrouille après 5 échecs dans les 30 dernières minutes")
        void locksAfterMediumThreshold() {
            List<LoginAttempt> attempts = buildFailures(5, NOW.minusSeconds(300));
            LockoutDecision decision = policy.evaluate(attempts, NOW);
            assertThat(decision.isLocked()).isTrue();
            assertThat(decision.remainingLockDuration()).hasValue(Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("Verrouillage dur")
    class HardLock {

        @Test
        @DisplayName("verrouille après 8 échecs dans la dernière heure")
        void locksAfterHardThreshold() {
            List<LoginAttempt> attempts = buildFailures(8, NOW.minusSeconds(1000));
            LockoutDecision decision = policy.evaluate(attempts, NOW);
            assertThat(decision.isLocked()).isTrue();
            assertThat(decision.remainingLockDuration()).hasValue(Duration.ofMinutes(30));
        }
    }

    @Nested
    @DisplayName("LockoutDecision Value Object")
    class LockoutDecisionTest {

        @Test
        @DisplayName("allowed() retourne une décision non verrouillée")
        void allowedDecision() {
            LockoutDecision d = LockoutDecision.allowed();
            assertThat(d.isAllowed()).isTrue();
            assertThat(d.isLocked()).isFalse();
            assertThat(d.remainingLockDuration()).isEmpty();
        }

        @Test
        @DisplayName("locked() retourne une décision verrouillée avec durée")
        void lockedDecision() {
            LockoutDecision d = LockoutDecision.locked(Duration.ofMinutes(10));
            assertThat(d.isLocked()).isTrue();
            assertThat(d.remainingLockDuration()).hasValue(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("locked() rejette une durée nulle")
        void lockedRejectsNullDuration() {
            assertThatThrownBy(() -> LockoutDecision.locked(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("locked() rejette une durée négative")
        void lockedRejectsNegativeDuration() {
            assertThatThrownBy(() -> LockoutDecision.locked(Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires de test
    // -------------------------------------------------------------------------

    private List<LoginAttempt> buildFailures(int count, Instant startTime) {
        List<LoginAttempt> attempts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            attempts.add(new LoginAttempt(IDENTIFIER, startTime.plusSeconds(i * 5), false));
        }
        return attempts;
    }
}
