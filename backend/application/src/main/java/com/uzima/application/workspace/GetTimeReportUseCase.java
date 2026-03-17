package com.uzima.application.workspace;

import com.uzima.application.workspace.port.out.TimeEntryRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.TimeEntry;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Rapport de temps d'un projet ou d'un utilisateur.
 */
public final class GetTimeReportUseCase {

    private final TimeEntryRepositoryPort timeEntryRepository;

    public GetTimeReportUseCase(TimeEntryRepositoryPort timeEntryRepository) {
        this.timeEntryRepository = Objects.requireNonNull(timeEntryRepository,
                "Le repository d'entrées de temps est obligatoire");
    }

    /**
     * Rapport de temps pour un projet.
     */
    public TimeReportView forProject(ProjectId projectId) {
        Objects.requireNonNull(projectId, "L'identifiant du projet est obligatoire");
        List<TimeEntry> entries = timeEntryRepository.findByProjectId(projectId);
        return new TimeReportView(entries);
    }

    // -------------------------------------------------------------------------
    // Vue de sortie
    // -------------------------------------------------------------------------

    /**
     * Rapport de temps agrégé.
     *
     * @param entries Liste des entrées de temps
     */
    public record TimeReportView(List<TimeEntry> entries) {

        /** Durée totale des entrées arrêtées. */
        public Duration totalDuration() {
            return entries.stream()
                    .map(TimeEntry::duration)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .reduce(Duration.ZERO, Duration::plus);
        }

        /** Nombre d'entrées en cours. */
        public long runningCount() {
            return entries.stream().filter(TimeEntry::isRunning).count();
        }

        /** Nombre d'entrées arrêtées. */
        public long stoppedCount() {
            return entries.stream().filter(TimeEntry::isStopped).count();
        }
    }
}
