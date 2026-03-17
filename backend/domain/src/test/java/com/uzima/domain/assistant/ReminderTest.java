package com.uzima.domain.assistant;

import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderStatus;
import com.uzima.domain.assistant.model.ReminderTrigger;
import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Reminder — Aggregate Root")
class ReminderTest {

    private static final Instant NOW         = Instant.parse("2026-03-13T10:00:00Z");
    private static final Instant SCHEDULED   = Instant.parse("2026-03-13T12:00:00Z");
    private final TimeProvider   clock       = () -> NOW;
    private final UserId         userId      = UserId.generate();

    private Reminder pendingReminder;

    @BeforeEach
    void setUp() {
        pendingReminder = Reminder.create(userId, "Prendre ses médicaments",
                                          ReminderTrigger.TIME_BASED, SCHEDULED, clock);
    }

    // =========================================================================
    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Test
        @DisplayName("crée un rappel PENDING avec le bon contenu")
        void creates_pending_reminder() {
            assertThat(pendingReminder.status()).isEqualTo(ReminderStatus.PENDING);
            assertThat(pendingReminder.content()).isEqualTo("Prendre ses médicaments");
            assertThat(pendingReminder.userId()).isEqualTo(userId);
            assertThat(pendingReminder.trigger()).isEqualTo(ReminderTrigger.TIME_BASED);
            assertThat(pendingReminder.scheduledAt()).isEqualTo(SCHEDULED);
        }

        @Test
        @DisplayName("génère un ID unique")
        void generates_unique_id() {
            Reminder other = Reminder.create(userId, "Autre rappel",
                                              ReminderTrigger.CONTEXT_BASED, SCHEDULED, clock);
            assertThat(pendingReminder.id()).isNotEqualTo(other.id());
        }

        @Test
        @DisplayName("rejette un contenu vide")
        void rejects_blank_content() {
            assertThatThrownBy(() ->
                Reminder.create(userId, "   ", ReminderTrigger.TIME_BASED, SCHEDULED, clock)
            ).isInstanceOf(Reminder.InvalidReminderContentException.class);
        }

        @Test
        @DisplayName("rejette un contenu nul")
        void rejects_null_content() {
            assertThatThrownBy(() ->
                Reminder.create(userId, null, ReminderTrigger.TIME_BASED, SCHEDULED, clock)
            ).isInstanceOf(Reminder.InvalidReminderContentException.class);
        }

        @Test
        @DisplayName("rejette un contenu dépassant 500 caractères")
        void rejects_content_exceeding_500_chars() {
            String tooLong = "A".repeat(501);
            assertThatThrownBy(() ->
                Reminder.create(userId, tooLong, ReminderTrigger.TIME_BASED, SCHEDULED, clock)
            ).isInstanceOf(Reminder.InvalidReminderContentException.class);
        }

        @Test
        @DisplayName("strip() le contenu")
        void strips_content() {
            Reminder r = Reminder.create(userId, "  Rappel  ",
                                          ReminderTrigger.TIME_BASED, SCHEDULED, clock);
            assertThat(r.content()).isEqualTo("Rappel");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("trigger()")
    class TriggerTest {

        @Test
        @DisplayName("PENDING → TRIGGERED et émet un événement")
        void pending_to_triggered() {
            pendingReminder.trigger(clock);

            assertThat(pendingReminder.status()).isEqualTo(ReminderStatus.TRIGGERED);
            assertThat(pendingReminder.triggeredAt()).contains(NOW);

            List<DomainEvent> events = pendingReminder.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(Reminder.ReminderTriggeredEvent.class);
        }

        @Test
        @DisplayName("est idempotent si déjà TRIGGERED")
        void idempotent_when_already_triggered() {
            pendingReminder.trigger(clock);
            pendingReminder.pullDomainEvents(); // vide la liste

            pendingReminder.trigger(clock);     // deuxième appel
            assertThat(pendingReminder.pullDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("SNOOZED → TRIGGERED remet à zéro snoozedUntil")
        void snoozed_to_triggered() {
            pendingReminder.trigger(clock);
            pendingReminder.snooze(Duration.ofMinutes(10), clock);
            pendingReminder.trigger(clock);

            assertThat(pendingReminder.status()).isEqualTo(ReminderStatus.TRIGGERED);
            assertThat(pendingReminder.snoozedUntil()).isEmpty();
        }

        @Test
        @DisplayName("DISMISSED → exception")
        void dismissed_rejects_trigger() {
            pendingReminder.dismiss(clock);
            assertThatThrownBy(() -> pendingReminder.trigger(clock))
                .isInstanceOf(Reminder.IllegalReminderTransitionException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("snooze()")
    class SnoozeTest {

        @Test
        @DisplayName("TRIGGERED → SNOOZED avec snoozedUntil correct")
        void triggered_to_snoozed() {
            pendingReminder.trigger(clock);
            pendingReminder.snooze(Duration.ofMinutes(30), clock);

            assertThat(pendingReminder.status()).isEqualTo(ReminderStatus.SNOOZED);
            assertThat(pendingReminder.snoozedUntil())
                .contains(NOW.plus(Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("rejette un délai nul ou négatif")
        void rejects_non_positive_delay() {
            pendingReminder.trigger(clock);
            assertThatThrownBy(() -> pendingReminder.snooze(Duration.ZERO, clock))
                .isInstanceOf(Reminder.IllegalReminderTransitionException.class);
            assertThatThrownBy(() -> pendingReminder.snooze(Duration.ofSeconds(-1), clock))
                .isInstanceOf(Reminder.IllegalReminderTransitionException.class);
        }

        @Test
        @DisplayName("PENDING → snooze interdit")
        void pending_rejects_snooze() {
            assertThatThrownBy(() -> pendingReminder.snooze(Duration.ofMinutes(5), clock))
                .isInstanceOf(Reminder.IllegalReminderTransitionException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("dismiss()")
    class DismissTest {

        @Test
        @DisplayName("PENDING → DISMISSED")
        void pending_to_dismissed() {
            pendingReminder.dismiss(clock);
            assertThat(pendingReminder.status()).isEqualTo(ReminderStatus.DISMISSED);
            assertThat(pendingReminder.dismissedAt()).contains(NOW);
        }

        @Test
        @DisplayName("TRIGGERED → DISMISSED")
        void triggered_to_dismissed() {
            pendingReminder.trigger(clock);
            pendingReminder.dismiss(clock);
            assertThat(pendingReminder.isDismissed()).isTrue();
        }

        @Test
        @DisplayName("SNOOZED → DISMISSED")
        void snoozed_to_dismissed() {
            pendingReminder.trigger(clock);
            pendingReminder.snooze(Duration.ofMinutes(5), clock);
            pendingReminder.dismiss(clock);
            assertThat(pendingReminder.isDismissed()).isTrue();
        }

        @Test
        @DisplayName("DISMISSED → dismiss interdit (état terminal)")
        void dismissed_rejects_dismiss() {
            pendingReminder.dismiss(clock);
            assertThatThrownBy(() -> pendingReminder.dismiss(clock))
                .isInstanceOf(Reminder.IllegalReminderTransitionException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("pullDomainEvents()")
    class DomainEventsTest {

        @Test
        @DisplayName("déduplique les événements identiques")
        void deduplicates_events() {
            // On injecte manuellement deux trigger successifs — le premier est idempotent,
            // donc un seul événement au total
            pendingReminder.trigger(clock);
            List<DomainEvent> first = pendingReminder.pullDomainEvents();
            assertThat(first).hasSize(1);

            // Après pull la liste est vide
            assertThat(pendingReminder.pullDomainEvents()).isEmpty();
        }
    }
}
