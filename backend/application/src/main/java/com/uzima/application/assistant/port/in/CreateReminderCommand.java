package com.uzima.application.assistant.port.in;

import com.uzima.domain.assistant.model.ReminderTrigger;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/** Commande : Créer un rappel. */
public record CreateReminderCommand(
        UserId          userId,
        String          content,
        ReminderTrigger trigger,
        Instant         scheduledAt
) {
    public CreateReminderCommand {
        Objects.requireNonNull(userId,      "userId est obligatoire");
        Objects.requireNonNull(content,     "content est obligatoire");
        Objects.requireNonNull(trigger,     "trigger est obligatoire");
        Objects.requireNonNull(scheduledAt, "scheduledAt est obligatoire");
    }
}
