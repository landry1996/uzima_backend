package com.uzima.domain.wellbeing;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.FocusSessionStatus;
import com.uzima.domain.wellbeing.model.InterruptionReason;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FocusSession — Aggregate Root")
class FocusSessionTest {

    private static final Instant START = Instant.parse("2026-03-13T09:00:00Z");
    private static final Instant END   = Instant.parse("2026-03-13T09:25:00Z");

    private final TimeProvider startClock = () -> START;
    private final TimeProvider endClock   = () -> END;
    private final UserId       userId     = UserId.generate();

    private FocusSession activeSession;

    @BeforeEach
    void setUp() {
        activeSession = FocusSession.start(userId, startClock);
    }

    // =========================================================================
    @Nested
    @DisplayName("start()")
    class StartTest {

        @Test
        @DisplayName("crée une session ACTIVE")
        void creates_active_session() {
            assertThat(activeSession.status()).isEqualTo(FocusSessionStatus.ACTIVE);
            assertThat(activeSession.isActive()).isTrue();
            assertThat(activeSession.startedAt()).isEqualTo(START);
            assertThat(activeSession.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("duration() vide si ACTIVE")
        void duration_empty_when_active() {
            assertThat(activeSession.duration()).isEmpty();
        }

        @Test
        @DisplayName("génère un ID unique par session")
        void generates_unique_ids() {
            FocusSession other = FocusSession.start(userId, startClock);
            assertThat(activeSession.id()).isNotEqualTo(other.id());
        }

        @Test
        @DisplayName("endedAt() vide si ACTIVE")
        void ended_at_empty_when_active() {
            assertThat(activeSession.endedAt()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("end()")
    class EndTest {

        @Test
        @DisplayName("ACTIVE → COMPLETED et émet un événement")
        void active_to_completed() {
            activeSession.end(endClock);

            assertThat(activeSession.isCompleted()).isTrue();
            assertThat(activeSession.endedAt()).contains(END);

            List<DomainEvent> events = activeSession.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(FocusSession.FocusSessionEndedEvent.class);
        }

        @Test
        @DisplayName("duration() = 25 minutes après end()")
        void duration_is_25_minutes() {
            activeSession.end(endClock);
            assertThat(activeSession.duration()).contains(Duration.ofMinutes(25));
        }

        @Test
        @DisplayName("COMPLETED → end() interdit")
        void completed_rejects_end() {
            activeSession.end(endClock);
            assertThatThrownBy(() -> activeSession.end(endClock))
                .isInstanceOf(FocusSession.AlreadyEndedException.class);
        }

        @Test
        @DisplayName("INTERRUPTED → end() interdit")
        void interrupted_rejects_end() {
            activeSession.interrupt(InterruptionReason.NOTIFICATION, endClock);
            assertThatThrownBy(() -> activeSession.end(endClock))
                .isInstanceOf(FocusSession.AlreadyEndedException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("interrupt()")
    class InterruptTest {

        @Test
        @DisplayName("ACTIVE → INTERRUPTED avec la bonne raison")
        void active_to_interrupted() {
            activeSession.interrupt(InterruptionReason.INCOMING_CALL, endClock);

            assertThat(activeSession.wasInterrupted()).isTrue();
            assertThat(activeSession.status()).isEqualTo(FocusSessionStatus.INTERRUPTED);
            assertThat(activeSession.interruptionReason()).contains(InterruptionReason.INCOMING_CALL);
            assertThat(activeSession.endedAt()).contains(END);
        }

        @Test
        @DisplayName("duration() correcte après interruption")
        void duration_correct_after_interrupt() {
            activeSession.interrupt(InterruptionReason.USER_CHOICE, endClock);
            assertThat(activeSession.duration()).contains(Duration.ofMinutes(25));
        }

        @Test
        @DisplayName("COMPLETED → interrupt() interdit")
        void completed_rejects_interrupt() {
            activeSession.end(endClock);
            assertThatThrownBy(() -> activeSession.interrupt(InterruptionReason.EMERGENCY, endClock))
                .isInstanceOf(FocusSession.AlreadyEndedException.class);
        }

        @Test
        @DisplayName("aucun événement émis lors d'une interruption")
        void no_event_on_interrupt() {
            activeSession.interrupt(InterruptionReason.TIMEOUT, endClock);
            assertThat(activeSession.pullDomainEvents()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("reconstitute()")
    class ReconstituteTest {

        @Test
        @DisplayName("reconstitue correctement une session terminée")
        void reconstitutes_completed_session() {
            FocusSession r = FocusSession.reconstitute(
                activeSession.id(), userId, START,
                FocusSessionStatus.COMPLETED, END, null
            );
            assertThat(r.isCompleted()).isTrue();
            assertThat(r.duration()).contains(Duration.ofMinutes(25));
            assertThat(r.interruptionReason()).isEmpty();
        }

        @Test
        @DisplayName("reconstitue correctement une session interrompue")
        void reconstitutes_interrupted_session() {
            FocusSession r = FocusSession.reconstitute(
                activeSession.id(), userId, START,
                FocusSessionStatus.INTERRUPTED, END, InterruptionReason.EMERGENCY
            );
            assertThat(r.wasInterrupted()).isTrue();
            assertThat(r.interruptionReason()).contains(InterruptionReason.EMERGENCY);
        }
    }
}
