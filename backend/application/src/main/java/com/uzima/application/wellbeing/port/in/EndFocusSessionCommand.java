package com.uzima.application.wellbeing.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.FocusSessionId;

import java.util.Objects;

/** Commande : Terminer normalement une session de focus. */
public record EndFocusSessionCommand(FocusSessionId sessionId, UserId userId) {
    public EndFocusSessionCommand {
        Objects.requireNonNull(sessionId, "sessionId est obligatoire");
        Objects.requireNonNull(userId,    "userId est obligatoire");
    }
}
