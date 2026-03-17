package com.uzima.bootstrap.adapter.http.assistant;

import com.uzima.application.assistant.CreateReminderUseCase;
import com.uzima.application.assistant.DismissReminderUseCase;
import com.uzima.application.assistant.GetRemindersUseCase;
import com.uzima.application.assistant.SnoozeReminderUseCase;
import com.uzima.application.assistant.port.in.CreateReminderCommand;
import com.uzima.application.assistant.port.in.DismissReminderCommand;
import com.uzima.application.assistant.port.in.SnoozeReminderCommand;
import com.uzima.bootstrap.adapter.http.dto.CreateReminderRequest;
import com.uzima.bootstrap.adapter.http.dto.ReminderResponse;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.assistant.model.ReminderStatus;
import com.uzima.domain.assistant.model.ReminderTrigger;
import com.uzima.domain.user.model.UserId;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
// UUID kept for @PathVariable UUID id

/**
 * REST Controller — Assistant IA : Rappels.
 * <p>
 * POST   /api/assistant/reminders             → 201 + id
 * GET    /api/assistant/reminders             → 200 + liste
 * GET    /api/assistant/reminders/active      → 200 + liste rappels actifs
 * POST   /api/assistant/reminders/{id}/snooze → 204
 * POST   /api/assistant/reminders/{id}/dismiss → 204
 */
@RestController
@RequestMapping("/api/assistant/reminders")
public class AssistantController {

    private final CreateReminderUseCase  createReminder;
    private final GetRemindersUseCase    getReminders;
    private final SnoozeReminderUseCase  snoozeReminder;
    private final DismissReminderUseCase dismissReminder;

    public AssistantController(CreateReminderUseCase createReminder,
                                GetRemindersUseCase getReminders,
                                SnoozeReminderUseCase snoozeReminder,
                                DismissReminderUseCase dismissReminder) {
        this.createReminder  = createReminder;
        this.getReminders    = getReminders;
        this.snoozeReminder  = snoozeReminder;
        this.dismissReminder = dismissReminder;
    }

    // -------------------------------------------------------------------------
    // POST /api/assistant/reminders
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<Map<String, String>> createReminder(
            @RequestBody CreateReminderRequest req
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        CreateReminderCommand cmd = new CreateReminderCommand(
            userId,
            req.content(),
            ReminderTrigger.valueOf(req.trigger()),
            Instant.parse(req.scheduledAt())
        );
        ReminderId id = createReminder.execute(cmd);
        return ResponseEntity
            .created(URI.create("/api/assistant/reminders/" + id))
            .body(Map.of("id", id.toString()));
    }

    // -------------------------------------------------------------------------
    // GET /api/assistant/reminders
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ReminderResponse>> listReminders(
            @RequestParam(required = false) String status
    ) {
        UserId uid = SecurityContextHelper.currentUserId();
        List<Reminder> reminders = status != null
            ? getReminders.findByStatus(uid, ReminderStatus.valueOf(status))
            : getReminders.findAll(uid);
        return ResponseEntity.ok(reminders.stream().map(ReminderResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // GET /api/assistant/reminders/active
    // -------------------------------------------------------------------------

    @GetMapping("/active")
    public ResponseEntity<List<ReminderResponse>> listActiveReminders() {
        UserId userId = SecurityContextHelper.currentUserId();
        List<Reminder> active = getReminders.findActive(userId);
        return ResponseEntity.ok(active.stream().map(ReminderResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // POST /api/assistant/reminders/{id}/snooze
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/snooze")
    public ResponseEntity<Void> snooze(
            @PathVariable UUID id,
            @RequestParam long delayMinutes
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        snoozeReminder.execute(new SnoozeReminderCommand(
            ReminderId.of(id), userId, Duration.ofMinutes(delayMinutes)
        ));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // POST /api/assistant/reminders/{id}/dismiss
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable UUID id) {
        UserId userId = SecurityContextHelper.currentUserId();
        dismissReminder.execute(new DismissReminderCommand(ReminderId.of(id), userId));
        return ResponseEntity.noContent().build();
    }

}
