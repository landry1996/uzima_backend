package com.uzima.application.wellbeing;

import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.application.wellbeing.port.out.UsageSessionRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.DigitalHealthMetrics;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.wellbeing.model.WellbeingReport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Use Case : Générer le rapport de bien-être numérique d'un utilisateur pour une période. */
public class GetWellbeingReportUseCase {

    private final FocusSessionRepositoryPort focusRepository;
    private final UsageSessionRepositoryPort usageRepository;

    public GetWellbeingReportUseCase(FocusSessionRepositoryPort focusRepository,
                                     UsageSessionRepositoryPort usageRepository) {
        this.focusRepository = Objects.requireNonNull(focusRepository);
        this.usageRepository = Objects.requireNonNull(usageRepository);
    }

    public WellbeingReport execute(UserId userId, LocalDate periodStart, LocalDate periodEnd) {
        Objects.requireNonNull(userId,      "userId est obligatoire");
        Objects.requireNonNull(periodStart, "periodStart est obligatoire");
        Objects.requireNonNull(periodEnd,   "periodEnd est obligatoire");

        Instant from = periodStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = periodEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<FocusSession> focusSessions = focusRepository.findByUserIdBetween(userId, from, to);
        List<UsageSession> usageSessions = usageRepository.findByUserIdBetween(userId, from, to);

        DigitalHealthMetrics metrics = DigitalHealthMetrics.compute(usageSessions, focusSessions);
        return WellbeingReport.of(userId, periodStart, periodEnd, metrics);
    }
}
