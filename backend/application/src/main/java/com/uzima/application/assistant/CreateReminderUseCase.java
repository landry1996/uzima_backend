package com.uzima.application.assistant;

import com.uzima.application.assistant.port.in.CreateReminderCommand;
import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/** Use Case : Créer un nouveau rappel pour un utilisateur. */
public class CreateReminderUseCase {

    private final ReminderRepositoryPort repository;
    private final TimeProvider           clock;

    public CreateReminderUseCase(ReminderRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public ReminderId execute(CreateReminderCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        Reminder reminder = Reminder.create(
            cmd.userId(), cmd.content(), cmd.trigger(), cmd.scheduledAt(), clock
        );
        repository.save(reminder);
        return reminder.id();
    }
}
