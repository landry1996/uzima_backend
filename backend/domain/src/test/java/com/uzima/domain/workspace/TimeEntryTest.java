package com.uzima.domain.workspace;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TimeEntry (Entity)")
class TimeEntryTest {

    private static final Instant START = Instant.parse("2026-03-13T09:00:00Z");
    private static final Instant STOP  = Instant.parse("2026-03-13T10:30:00Z");

    private final TimeProvider startClock = () -> START;
    private final TimeProvider stopClock  = () -> STOP;

    private UserId  alice;
    private Project project;

    @BeforeEach
    void setUp() {
        alice   = UserId.generate();
        project = Project.create("Test Project", alice, startClock);
    }

    @Nested
    @DisplayName("startTimeEntry() → start()")
    class StartTest {

        @Test
        @DisplayName("l'entrée est en cours au démarrage")
        void isRunningAfterStart() {
            TimeEntry entry = project.startTimeEntry(alice, "Travail", startClock);

            assertThat(entry.isRunning()).isTrue();
            assertThat(entry.isStopped()).isFalse();
            assertThat(entry.startedAt()).isEqualTo(START);
            assertThat(entry.stoppedAt()).isEmpty();
            assertThat(entry.duration()).isEmpty();
        }

        @Test
        @DisplayName("description est accessible")
        void descriptionIsAccessible() {
            TimeEntry entry = project.startTimeEntry(alice, "Développement feature X", startClock);

            assertThat(entry.description()).contains("Développement feature X");
        }

        @Test
        @DisplayName("description peut être null")
        void descriptionCanBeNull() {
            TimeEntry entry = project.startTimeEntry(alice, null, startClock);

            assertThat(entry.description()).isEmpty();
        }

        @Test
        @DisplayName("projectId correspond au projet")
        void projectIdMatches() {
            TimeEntry entry = project.startTimeEntry(alice, "Travail", startClock);

            assertThat(entry.projectId()).isEqualTo(project.id());
            assertThat(entry.userId()).isEqualTo(alice);
        }
    }

    @Nested
    @DisplayName("stop()")
    class StopTest {

        @Test
        @DisplayName("arrête l'entrée et calcule la durée")
        void stopsAndCalculatesDuration() {
            TimeEntry entry = project.startTimeEntry(alice, "Travail", startClock);

            entry.stop(stopClock);

            assertThat(entry.isRunning()).isFalse();
            assertThat(entry.isStopped()).isTrue();
            assertThat(entry.stoppedAt()).contains(STOP);
            assertThat(entry.duration()).contains(Duration.ofMinutes(90));
        }

        @Test
        @DisplayName("lève AlreadyStoppedException si déjà arrêtée")
        void throwsWhenAlreadyStopped() {
            TimeEntry entry = project.startTimeEntry(alice, "Travail", startClock);
            entry.stop(stopClock);

            assertThatThrownBy(() -> entry.stop(stopClock))
                    .isInstanceOf(TimeEntry.AlreadyStoppedException.class);
        }
    }

    @Nested
    @DisplayName("duration()")
    class DurationTest {

        @Test
        @DisplayName("retourne empty si entrée en cours")
        void emptyWhenRunning() {
            TimeEntry entry = project.startTimeEntry(alice, "Travail", startClock);

            assertThat(entry.duration()).isEmpty();
        }

        @Test
        @DisplayName("retourne la durée exacte après arrêt")
        void exactDurationAfterStop() {
            TimeEntry entry = project.startTimeEntry(alice, "Travail", startClock);
            entry.stop(stopClock);

            assertThat(entry.duration())
                    .isPresent()
                    .hasValueSatisfying(d -> assertThat(d).isEqualTo(Duration.between(START, STOP)));
        }
    }
}
