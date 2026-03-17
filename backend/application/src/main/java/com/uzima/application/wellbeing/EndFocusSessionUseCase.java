package com.uzima.application.wellbeing;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.wellbeing.port.in.EndFocusSessionCommand;
import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.wellbeing.model.FocusSession;

import java.util.Objects;

/** Use Case : Terminer normalement une session de focus (ACTIVE → COMPLETED). */
public class EndFocusSessionUseCase {

    private final FocusSessionRepositoryPort repository;
    private final TimeProvider               clock;

    public EndFocusSessionUseCase(FocusSessionRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public void execute(EndFocusSessionCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        FocusSession session = repository.findById(cmd.sessionId())
            .orElseThrow(() -> ResourceNotFoundException.focusSessionNotFound(cmd.sessionId()));

        if (!session.userId().equals(cmd.userId())) {
            throw UnauthorizedException.notFocusSessionOwner(cmd.sessionId());
        }

        session.end(clock);
        repository.save(session);
    }
}
