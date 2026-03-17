package com.uzima.bootstrap.adapter.http.wellbeing;

import com.uzima.application.wellbeing.EndFocusSessionUseCase;
import com.uzima.application.wellbeing.GetFocusSessionHistoryUseCase;
import com.uzima.application.wellbeing.GetUsageSessionsByAppTypeUseCase;
import com.uzima.application.wellbeing.GetWellbeingReportUseCase;
import com.uzima.application.wellbeing.InterruptFocusSessionUseCase;
import com.uzima.application.wellbeing.StartFocusSessionUseCase;
import com.uzima.application.wellbeing.TrackAppUsageUseCase;
import com.uzima.application.wellbeing.port.in.EndFocusSessionCommand;
import com.uzima.application.wellbeing.port.in.InterruptFocusSessionCommand;
import com.uzima.application.wellbeing.port.in.StartFocusSessionCommand;
import com.uzima.application.wellbeing.port.in.TrackAppUsageCommand;
import com.uzima.domain.user.model.UserId;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.wellbeing.model.AppType;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.wellbeing.model.FocusSessionId;
import com.uzima.domain.wellbeing.model.FocusSessionStatus;
import com.uzima.domain.wellbeing.model.InterruptionReason;
import com.uzima.domain.wellbeing.model.WellbeingReport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller — Bien-être numérique.
 * <p>
 * GET  /api/wellbeing/report                   → rapport de santé digitale
 * POST /api/wellbeing/focus                    → démarrer une session focus
 * GET  /api/wellbeing/focus/history            → historique des sessions par statut
 * POST /api/wellbeing/focus/{id}/end           → terminer une session focus
 * POST /api/wellbeing/focus/{id}/interrupt     → interrompre une session focus
 * POST /api/wellbeing/usage                    → enregistrer une session d'utilisation d'app
 * GET  /api/wellbeing/usage/history             → sessions d'utilisation filtrées par type d'app
 */
@RestController
@RequestMapping("/api/wellbeing")
public class WellbeingController {

    private final GetWellbeingReportUseCase    getReport;
    private final StartFocusSessionUseCase     startFocus;
    private final EndFocusSessionUseCase       endFocus;
    private final InterruptFocusSessionUseCase interruptFocus;
    private final TrackAppUsageUseCase         trackUsage;
    private final GetFocusSessionHistoryUseCase   getFocusHistory;
    private final GetUsageSessionsByAppTypeUseCase getUsageByAppType;

    public WellbeingController(GetWellbeingReportUseCase getReport,
                                StartFocusSessionUseCase startFocus,
                                EndFocusSessionUseCase endFocus,
                                InterruptFocusSessionUseCase interruptFocus,
                                TrackAppUsageUseCase trackUsage,
                                GetFocusSessionHistoryUseCase getFocusHistory,
                                GetUsageSessionsByAppTypeUseCase getUsageByAppType) {
        this.getReport         = getReport;
        this.startFocus        = startFocus;
        this.endFocus          = endFocus;
        this.interruptFocus    = interruptFocus;
        this.trackUsage        = trackUsage;
        this.getFocusHistory   = getFocusHistory;
        this.getUsageByAppType = getUsageByAppType;
    }

    // -------------------------------------------------------------------------
    // GET /api/wellbeing/report
    // -------------------------------------------------------------------------

    @GetMapping("/report")
    public ResponseEntity<WellbeingReportResponse> getReport(
            @RequestParam String from,
            @RequestParam String to
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        WellbeingReport report = getReport.execute(
            userId, LocalDate.parse(from), LocalDate.parse(to)
        );
        return ResponseEntity.ok(WellbeingReportResponse.from(report));
    }

    // -------------------------------------------------------------------------
    // POST /api/wellbeing/focus
    // -------------------------------------------------------------------------

    @PostMapping("/focus")
    public ResponseEntity<Map<String, String>> startFocus() {
        UserId userId = SecurityContextHelper.currentUserId();
        FocusSessionId id = startFocus.execute(new StartFocusSessionCommand(userId));
        return ResponseEntity
            .created(URI.create("/api/wellbeing/focus/" + id))
            .body(Map.of("id", id.toString()));
    }

    // -------------------------------------------------------------------------
    // GET /api/wellbeing/focus/history?userId=...&status=COMPLETED|INTERRUPTED
    // -------------------------------------------------------------------------

    @GetMapping("/focus/history")
    public ResponseEntity<List<FocusSessionResponse>> getFocusHistory(
            @RequestParam String status
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        List<FocusSession> sessions = getFocusHistory.execute(
            userId, FocusSessionStatus.valueOf(status)
        );
        return ResponseEntity.ok(sessions.stream().map(FocusSessionResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // POST /api/wellbeing/focus/{id}/end
    // -------------------------------------------------------------------------

    @PostMapping("/focus/{id}/end")
    public ResponseEntity<Void> endFocus(@PathVariable UUID id) {
        UserId userId = SecurityContextHelper.currentUserId();
        endFocus.execute(new EndFocusSessionCommand(FocusSessionId.of(id), userId));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // POST /api/wellbeing/focus/{id}/interrupt
    // -------------------------------------------------------------------------

    @PostMapping("/focus/{id}/interrupt")
    public ResponseEntity<Void> interruptFocus(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        interruptFocus.execute(new InterruptFocusSessionCommand(
            FocusSessionId.of(id), userId, InterruptionReason.valueOf(reason)
        ));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // POST /api/wellbeing/usage
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // GET /api/wellbeing/usage/history?userId=...&appType=SOCIAL|PRODUCTIVITY|...
    // -------------------------------------------------------------------------

    @GetMapping("/usage/history")
    public ResponseEntity<List<UsageSessionResponse>> getUsageByAppType(
            @RequestParam String appType
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        List<UsageSession> sessions = getUsageByAppType.execute(
            userId, AppType.valueOf(appType)
        );
        return ResponseEntity.ok(sessions.stream().map(UsageSessionResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // POST /api/wellbeing/usage
    // -------------------------------------------------------------------------

    @PostMapping("/usage")
    public ResponseEntity<Map<String, String>> trackUsage(@RequestBody TrackUsageRequest req) {
        UserId userId = SecurityContextHelper.currentUserId();
        var sessionId = trackUsage.execute(new TrackAppUsageCommand(
            userId, req.appName(), AppType.valueOf(req.appType())
        ));
        return ResponseEntity
            .created(URI.create("/api/wellbeing/usage/" + sessionId))
            .body(Map.of("id", sessionId.toString()));
    }

    // -------------------------------------------------------------------------
    // DTOs internes
    // -------------------------------------------------------------------------

    record TrackUsageRequest(String appName, String appType) {}

    record UsageSessionResponse(
            String id,
            String userId,
            String appName,
            String appType,
            Instant startedAt,
            Instant endedAt,
            Long durationMinutes
    ) {
        static UsageSessionResponse from(UsageSession s) {
            return new UsageSessionResponse(
                s.id().toString(),
                s.userId().toString(),
                s.appName(),
                s.appType().name(),
                s.startedAt(),
                s.endedAt().orElse(null),
                s.duration().map(Duration::toMinutes).orElse(null)
            );
        }
    }

    record FocusSessionResponse(
            String id,
            String userId,
            String status,
            Instant startedAt,
            Instant endedAt,
            Long durationMinutes,
            String interruptionReason
    ) {
        static FocusSessionResponse from(FocusSession s) {
            return new FocusSessionResponse(
                s.id().toString(),
                s.userId().toString(),
                s.status().name(),
                s.startedAt(),
                s.endedAt().orElse(null),
                s.duration().map(Duration::toMinutes).orElse(null),
                s.interruptionReason().map(Enum::name).orElse(null)
            );
        }
    }

    record WellbeingReportResponse(
            String userId,
            String periodStart,
            String periodEnd,
            long totalScreenTimeMinutes,
            long productiveTimeMinutes,
            long distractingTimeMinutes,
            long focusSessionCount,
            long totalFocusTimeMinutes,
            long interruptedFocusCount,
            int healthScore,
            String summary
    ) {
        static WellbeingReportResponse from(WellbeingReport r) {
            var m = r.metrics();
            return new WellbeingReportResponse(
                r.userId().toString(),
                r.periodStart().toString(),
                r.periodEnd().toString(),
                toMinutes(m.totalScreenTime()),
                toMinutes(m.productiveTime()),
                toMinutes(m.distractingTime()),
                m.focusSessionCount(),
                toMinutes(m.totalFocusTime()),
                m.interruptedFocusCount(),
                r.healthScore(),
                r.summary()
            );
        }

        private static long toMinutes(Duration d) { return d.toMinutes(); }
    }
}
