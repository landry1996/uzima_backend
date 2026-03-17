package com.uzima.application.assistant.port.in;

import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Ignorer un rappel. */
public record DismissReminderCommand(ReminderId reminderId, UserId userId) {
    public DismissReminderCommand {
        Objects.requireNonNull(reminderId, "reminderId est obligatoire");
        Objects.requireNonNull(userId,     "userId est obligatoire");
    }
}
