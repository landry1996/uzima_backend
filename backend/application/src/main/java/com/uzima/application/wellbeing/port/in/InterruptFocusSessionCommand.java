package com.uzima.application.wellbeing.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.FocusSessionId;
import com.uzima.domain.wellbeing.model.InterruptionReason;

import java.util.Objects;

/** Commande : Interrompre une session de focus. */
public record InterruptFocusSessionCommand(
        FocusSessionId    sessionId,
        UserId            userId,
        InterruptionReason reason
) {
    public InterruptFocusSessionCommand {
        Objects.requireNonNull(sessionId, "sessionId est obligatoire");
        Objects.requireNonNull(userId,    "userId est obligatoire");
        Objects.requireNonNull(reason,    "reason est obligatoire");
    }
}
