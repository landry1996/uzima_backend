package com.uzima.application.assistant.port.in;

import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.user.model.UserId;

import java.time.Duration;
import java.util.Objects;

/** Commande : Reporter un rappel. */
public record SnoozeReminderCommand(
        ReminderId reminderId,
        UserId     userId,
        Duration   delay
) {
    public SnoozeReminderCommand {
        Objects.requireNonNull(reminderId, "reminderId est obligatoire");
        Objects.requireNonNull(userId,     "userId est obligatoire");
        Objects.requireNonNull(delay,      "delay est obligatoire");
    }
}
