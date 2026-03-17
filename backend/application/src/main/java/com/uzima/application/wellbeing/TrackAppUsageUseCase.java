package com.uzima.application.wellbeing;

import com.uzima.application.wellbeing.port.in.TrackAppUsageCommand;
import com.uzima.application.wellbeing.port.out.UsageSessionRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.wellbeing.model.UsageSessionId;

import java.util.Objects;

/** Use Case : Enregistrer une session d'utilisation d'application. */
public class TrackAppUsageUseCase {

    private final UsageSessionRepositoryPort repository;
    private final TimeProvider               clock;

    public TrackAppUsageUseCase(UsageSessionRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public UsageSessionId execute(TrackAppUsageCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        UsageSession session = UsageSession.track(cmd.userId(), cmd.appName(), cmd.appType(), clock);
        repository.save(session);
        return session.id();
    }
}
