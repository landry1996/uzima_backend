package com.uzima.application.assistant;

import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Déclencher un rappel (PENDING/SNOOZED → TRIGGERED).
 * <p>
 * Appelé par un scheduler ou un service de géolocalisation selon le type de déclencheur.
 */
public class TriggerReminderUseCase {

    private final ReminderRepositoryPort repository;
    private final TimeProvider           clock;

    public TriggerReminderUseCase(ReminderRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public void execute(ReminderId reminderId) {
        Objects.requireNonNull(reminderId, "reminderId est obligatoire");

        Reminder reminder = repository.findById(reminderId)
            .orElseThrow(() -> ResourceNotFoundException.reminderNotFound(reminderId));

        reminder.trigger(clock);
        repository.save(reminder);
    }
}
