package com.uzima.application.workspace;

import com.uzima.application.workspace.port.out.TimeEntryRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.TimeEntry;

import java.util.Objects;
import java.util.Optional;

/**
 * Use Case : Récupérer l'entrée de temps en cours d'un utilisateur.
 * Utile pour afficher le chronomètre actif dans l'interface et empêcher
 * de démarrer une deuxième entrée en parallèle.
 */
public class GetRunningTimeEntryUseCase {

    private final TimeEntryRepositoryPort timeEntryRepository;

    public GetRunningTimeEntryUseCase(TimeEntryRepositoryPort timeEntryRepository) {
        this.timeEntryRepository = Objects.requireNonNull(timeEntryRepository, "Le repository de time entries est obligatoire");
    }

    public Optional<TimeEntry> execute(UserId userId) {
        Objects.requireNonNull(userId, "userId est obligatoire");
        return timeEntryRepository.findRunningForUser(userId);
    }
}
