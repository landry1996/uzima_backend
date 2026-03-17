package com.uzima.application.assistant;

import com.uzima.application.assistant.port.in.SnoozeReminderCommand;
import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/** Use Case : Reporter un rappel (TRIGGERED → SNOOZED). */
public class SnoozeReminderUseCase {

    private final ReminderRepositoryPort repository;
    private final TimeProvider           clock;

    public SnoozeReminderUseCase(ReminderRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public void execute(SnoozeReminderCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        Reminder reminder = repository.findById(cmd.reminderId())
            .orElseThrow(() -> ResourceNotFoundException.reminderNotFound(cmd.reminderId()));

        if (!reminder.userId().equals(cmd.userId())) {
            throw UnauthorizedException.notReminderOwner(cmd.reminderId());
        }

        reminder.snooze(cmd.delay(), clock);
        repository.save(reminder);
    }
}
