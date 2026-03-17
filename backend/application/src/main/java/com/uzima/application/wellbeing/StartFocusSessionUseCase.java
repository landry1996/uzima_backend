package com.uzima.application.wellbeing;

import com.uzima.application.shared.exception.ConflictException;
import com.uzima.application.wellbeing.port.in.StartFocusSessionCommand;
import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.FocusSessionId;

import java.util.Objects;

/** Use Case : Démarrer une session de focus (une seule active à la fois). */
public class StartFocusSessionUseCase {

    private final FocusSessionRepositoryPort repository;
    private final TimeProvider               clock;

    public StartFocusSessionUseCase(FocusSessionRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public FocusSessionId execute(StartFocusSessionCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        repository.findActiveByUserId(cmd.userId()).ifPresent(existing -> {
            throw ConflictException.focusSessionAlreadyActive(existing.id());
        });

        FocusSession session = FocusSession.start(cmd.userId(), clock);
        repository.save(session);
        return session.id();
    }
}
