package com.uzima.application.workspace;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.workspace.port.in.StartTimeEntryCommand;
import com.uzima.application.workspace.port.in.StopTimeEntryCommand;
import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.application.workspace.port.out.TimeEntryRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.workspace.model.TimeEntryId;

import java.util.Objects;

/**
 * Use Case : Gestion du time tracking (démarrage et arrêt).
 * <p>
 * Deux opérations :
 * - startEntry() : délègue à Project.startTimeEntry()
 * - stopEntry()  : récupère l'entrée, vérifie que userId correspond, stop()
 */
public final class TrackTimeUseCase {

    private final ProjectRepositoryPort   projectRepository;
    private final TimeEntryRepositoryPort timeEntryRepository;
    private final TimeProvider            clock;

    public TrackTimeUseCase(
            ProjectRepositoryPort projectRepository,
            TimeEntryRepositoryPort timeEntryRepository,
            TimeProvider clock
    ) {
        this.projectRepository   = Objects.requireNonNull(projectRepository,   "Le repository de projets est obligatoire");
        this.timeEntryRepository = Objects.requireNonNull(timeEntryRepository, "Le repository d'entrées de temps est obligatoire");
        this.clock               = Objects.requireNonNull(clock,               "Le fournisseur de temps est obligatoire");
    }

    /**
     * Démarre une entrée de temps dans un projet.
     *
     * @return L'identifiant de l'entrée créée
     * @throws ResourceNotFoundException             si le projet est introuvable
     * @throws Project.ProjectMembershipRequiredException si l'utilisateur n'est pas membre
     * @throws Project.ActiveTimeEntryExistsException    si une entrée est déjà en cours
     */
    public TimeEntryId startEntry(StartTimeEntryCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire");

        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> ResourceNotFoundException.projectNotFound(command.projectId()));

        TimeEntry entry = project.startTimeEntry(command.userId(), command.description(), clock);

        projectRepository.save(project);
        timeEntryRepository.save(entry);

        return entry.id();
    }

    /**
     * Arrête une entrée de temps.
     *
     * @throws ResourceNotFoundException           si l'entrée est introuvable
     * @throws UnauthorizedException               si l'utilisateur n'est pas le propriétaire de l'entrée
     * @throws TimeEntry.AlreadyStoppedException   si l'entrée est déjà arrêtée
     */
    public void stopEntry(StopTimeEntryCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire");

        TimeEntry entry = timeEntryRepository.findById(command.timeEntryId())
                .orElseThrow(() -> ResourceNotFoundException.timeEntryNotFound(command.timeEntryId()));

        if (!entry.userId().equals(command.userId())) {
            throw UnauthorizedException.cannotStopOthersTimeEntry(command.timeEntryId());
        }

        entry.stop(clock);
        timeEntryRepository.save(entry);
    }
}
