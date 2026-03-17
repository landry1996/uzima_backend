package com.uzima.domain.wellbeing.model;

import com.uzima.domain.user.model.UserId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object : Rapport de bien-être numérique pour une période donnée.
 * <p>
 * Immuable — assemblé depuis les métriques calculées.
 */
public record WellbeingReport(
        UserId               userId,
        LocalDate            periodStart,
        LocalDate            periodEnd,
        DigitalHealthMetrics metrics
) {

    public WellbeingReport {
        Objects.requireNonNull(userId,      "userId est obligatoire");
        Objects.requireNonNull(periodStart, "periodStart est obligatoire");
        Objects.requireNonNull(periodEnd,   "periodEnd est obligatoire");
        Objects.requireNonNull(metrics,     "metrics est obligatoire");
        if (periodEnd.isBefore(periodStart)) {
            throw new InvalidPeriodException("periodEnd ne peut pas être antérieure à periodStart");
        }
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static WellbeingReport of(UserId userId, LocalDate periodStart, LocalDate periodEnd,
                                     DigitalHealthMetrics metrics) {
        return new WellbeingReport(userId, periodStart, periodEnd, metrics);
    }

    // -------------------------------------------------------------------------
    // Computed helpers
    // -------------------------------------------------------------------------

    public int healthScore() { return metrics.healthScore(); }

    public String summary() {
        int score = healthScore();
        if (score >= 80) return "Excellent équilibre numérique";
        if (score >= 60) return "Bon équilibre numérique";
        if (score >= 40) return "Équilibre numérique à améliorer";
        return "Usage numérique préoccupant";
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    public static final class InvalidPeriodException extends RuntimeException {
        public InvalidPeriodException(String message) { super(message); }
    }
}
