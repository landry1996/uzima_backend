package com.uzima.domain.wellbeing.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Value Object : Métriques de santé digitale calculées à partir des sessions.
 * <p>
 * Immuable — calculé à la demande depuis les sessions d'utilisation.
 */
public record DigitalHealthMetrics(
        Duration totalScreenTime,
        Duration productiveTime,
        Duration distractingTime,
        long     focusSessionCount,
        Duration totalFocusTime,
        long     interruptedFocusCount,
        Map<AppType, Duration> timeByCategory
) {

    public DigitalHealthMetrics {
        Objects.requireNonNull(totalScreenTime,  "totalScreenTime est obligatoire");
        Objects.requireNonNull(productiveTime,   "productiveTime est obligatoire");
        Objects.requireNonNull(distractingTime,  "distractingTime est obligatoire");
        Objects.requireNonNull(totalFocusTime,   "totalFocusTime est obligatoire");
        Objects.requireNonNull(timeByCategory,   "timeByCategory est obligatoire");
        if (focusSessionCount < 0) throw new IllegalArgumentException("focusSessionCount ne peut pas être négatif");
        if (interruptedFocusCount < 0) throw new IllegalArgumentException("interruptedFocusCount ne peut pas être négatif");
        timeByCategory = Map.copyOf(timeByCategory);
    }

    // -------------------------------------------------------------------------
    // Factory : compute()
    // -------------------------------------------------------------------------

    /** Calcule les métriques à partir des listes de sessions brutes. */
    public static DigitalHealthMetrics compute(List<UsageSession> usageSessions,
                                               List<FocusSession> focusSessions) {
        Objects.requireNonNull(usageSessions, "usageSessions est obligatoire");
        Objects.requireNonNull(focusSessions, "focusSessions est obligatoire");

        Duration totalScreen = usageSessions.stream()
                .map(s -> s.duration().orElse(Duration.ZERO))
                .reduce(Duration.ZERO, Duration::plus);

        Duration productive = usageSessions.stream()
                .filter(UsageSession::isProductiveApp)
                .map(s -> s.duration().orElse(Duration.ZERO))
                .reduce(Duration.ZERO, Duration::plus);

        Duration distracting = totalScreen.minus(productive);

        Map<AppType, Duration> byCategory = usageSessions.stream()
                .collect(Collectors.groupingBy(
                    UsageSession::appType,
                    Collectors.reducing(Duration.ZERO,
                        s -> s.duration().orElse(Duration.ZERO),
                        Duration::plus)
                ));

        long focusCount = focusSessions.size();
        Duration totalFocus = focusSessions.stream()
                .map(s -> s.duration().orElse(Duration.ZERO))
                .reduce(Duration.ZERO, Duration::plus);
        long interrupted = focusSessions.stream().filter(FocusSession::wasInterrupted).count();

        return new DigitalHealthMetrics(
            totalScreen, productive, distracting,
            focusCount, totalFocus, interrupted, byCategory
        );
    }

    // -------------------------------------------------------------------------
    // Computed helpers
    // -------------------------------------------------------------------------

    /** Score de santé digitale de 0 à 100. */
    public int healthScore() {
        if (totalScreenTime.isZero()) return 100;

        long totalSecs      = totalScreenTime.toSeconds();
        long productiveSecs = productiveTime.toSeconds();
        long focusSecs      = totalFocusTime.toSeconds();

        double productiveRatio = (double) productiveSecs / totalSecs;
        double focusBonus      = Math.min(focusSecs / 3600.0, 1.0) * 20; // max +20 pts pour 1h focus
        double interruptPenalty= interruptedFocusCount * 5;               // -5 pts par interruption

        int score = (int) Math.round(productiveRatio * 80 + focusBonus - interruptPenalty);
        return Math.max(0, Math.min(100, score));
    }

}
