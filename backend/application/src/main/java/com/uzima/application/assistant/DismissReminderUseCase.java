package com.uzima.application.assistant;

import com.uzima.application.assistant.port.in.DismissReminderCommand;
import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/** Use Case : Ignorer un rappel (tout état non-terminal → DISMISSED). */
public class DismissReminderUseCase {

    private final ReminderRepositoryPort repository;
    private final TimeProvider           clock;

    public DismissReminderUseCase(ReminderRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public void execute(DismissReminderCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        Reminder reminder = repository.findById(cmd.reminderId())
            .orElseThrow(() -> ResourceNotFoundException.reminderNotFound(cmd.reminderId()));

        if (!reminder.userId().equals(cmd.userId())) {
            throw UnauthorizedException.notReminderOwner(cmd.reminderId());
        }

        reminder.dismiss(clock);
        repository.save(reminder);
    }
}
