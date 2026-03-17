package com.uzima.application.wellbeing;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.wellbeing.port.in.InterruptFocusSessionCommand;
import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.wellbeing.model.FocusSession;

import java.util.Objects;

/** Use Case : Interrompre une session de focus (ACTIVE → INTERRUPTED). */
public class InterruptFocusSessionUseCase {

    private final FocusSessionRepositoryPort repository;
    private final TimeProvider               clock;

    public InterruptFocusSessionUseCase(FocusSessionRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public void execute(InterruptFocusSessionCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        FocusSession session = repository.findById(cmd.sessionId())
            .orElseThrow(() -> ResourceNotFoundException.focusSessionNotFound(cmd.sessionId()));

        if (!session.userId().equals(cmd.userId())) {
            throw UnauthorizedException.notFocusSessionOwner(cmd.sessionId());
        }

        session.interrupt(cmd.reason(), clock);
        repository.save(session);
    }
}
