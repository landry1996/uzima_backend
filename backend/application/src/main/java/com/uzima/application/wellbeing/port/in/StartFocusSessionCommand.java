package com.uzima.application.wellbeing.port.in;

import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Démarrer une session de focus. */
public record StartFocusSessionCommand(UserId userId) {
    public StartFocusSessionCommand {
        Objects.requireNonNull(userId, "userId est obligatoire");
    }
}
