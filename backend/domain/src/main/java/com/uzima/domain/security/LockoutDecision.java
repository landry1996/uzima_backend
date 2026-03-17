package com.uzima.domain.security;

import java.time.Duration;
import java.util.Optional;

/**
 * Value Object : Décision de verrouillage retournée par AccountLockoutPolicy.
 * Encapsule le résultat de l'évaluation :
 * - allowed : l'authentification peut continuer
 * - locked : le compte est verrouillé pour une durée donnée
 * Immuable. Pas de setters.
 */
public final class LockoutDecision {

    private final boolean locked;
    private final Duration remainingLockDuration; // null si not locked

    private LockoutDecision(boolean locked, Duration remainingLockDuration) {
        this.locked = locked;
        this.remainingLockDuration = remainingLockDuration;
    }

    public static LockoutDecision allowed() {
        return new LockoutDecision(false, null);
    }

    public static LockoutDecision locked(Duration remainingDuration) {
        if (remainingDuration == null || remainingDuration.isNegative() || remainingDuration.isZero()) {
            throw new IllegalArgumentException("La durée de verrouillage doit être positive");
        }
        return new LockoutDecision(true, remainingDuration);
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isAllowed() {
        return !locked;
    }

    public Optional<Duration> remainingLockDuration() {
        return Optional.ofNullable(remainingLockDuration);
    }

    @Override
    public String toString() {
        return locked
            ? "LockoutDecision{LOCKED for " + remainingLockDuration + "}"
            : "LockoutDecision{ALLOWED}";
    }
}
