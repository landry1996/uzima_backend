package com.uzima.domain.workspace;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.ProjectId;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskPriority;
import com.uzima.domain.workspace.model.TaskStatus;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Task (Aggregate Root)")
class TaskTest {

    private static final Instant NOW   = Instant.parse("2026-03-13T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-13T10:30:00Z");

    private final TimeProvider clock      = () -> NOW;
    private final TimeProvider laterClock = () -> LATER;

    private UserId    alice;
    private ProjectId projectId;

    @BeforeEach
    void setUp() {
        alice     = UserId.generate();
        projectId = ProjectId.generate();
    }

    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Test
        @DisplayName("crée une tâche en état BACKLOG")
        void createsTaskInBacklog() {
            Task task = Task.create("Implémenter le login", projectId, alice, TaskPriority.HIGH, clock);

            assertThat(task.id()).isNotNull();
            assertThat(task.title()).isEqualTo("Implémenter le login");
            assertThat(task.status()).isEqualTo(TaskStatus.BACKLOG);
            assertThat(task.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(task.projectId()).isEqualTo(projectId);
            assertThat(task.createdAt()).isEqualTo(NOW);
            assertThat(task.isBlocked()).isFalse();
        }

        @Test
        @DisplayName("émet TaskStatusChangedEvent à la création")
        void emitsCreationEvent() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.LOW, clock);
            List<DomainEvent> events = task.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Task.TaskStatusChangedEvent.class);
            Task.TaskStatusChangedEvent event = (Task.TaskStatusChangedEvent) events.get(0);
            assertThat(event.newStatus()).isEqualTo(TaskStatus.BACKLOG);
            assertThat(event.previousStatus()).isNull();
        }

        @Test
        @DisplayName("lève InvalidTaskTitleException si titre vide")
        void throwsWhenTitleBlank() {
            assertThatThrownBy(() -> Task.create("  ", projectId, alice, TaskPriority.LOW, clock))
                    .isInstanceOf(Task.InvalidTaskTitleException.class);
        }

        @Test
        @DisplayName("strip() le titre")
        void stripsTitleOnCreate() {
            Task task = Task.create("  Ma tâche  ", projectId, alice, TaskPriority.LOW, clock);
            assertThat(task.title()).isEqualTo("Ma tâche");
        }
    }

    @Nested
    @DisplayName("start()")
    class StartTest {

        @Test
        @DisplayName("BACKLOG → IN_PROGRESS")
        void startsFromBacklog() {
            Task task = Task.create("Tâche", projectId, null, TaskPriority.MEDIUM, clock);
            task.pullDomainEvents();

            task.start(alice, laterClock);

            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.assigneeId()).contains(alice);
            assertThat(task.updatedAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("émet TaskStatusChangedEvent")
        void emitsEvent() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.HIGH, clock);
            task.pullDomainEvents();
            task.start(alice, laterClock);

            List<DomainEvent> events = task.pullDomainEvents();
            assertThat(events).hasSize(1);
            Task.TaskStatusChangedEvent event = (Task.TaskStatusChangedEvent) events.get(0);
            assertThat(event.previousStatus()).isEqualTo(TaskStatus.BACKLOG);
            assertThat(event.newStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si déjà DONE")
        void throwsWhenDone() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.HIGH, clock);
            task.start(alice, clock);
            task.complete(clock);

            assertThatThrownBy(() -> task.start(alice, laterClock))
                    .isInstanceOf(Task.IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("complete()")
    class CompleteTest {

        @Test
        @DisplayName("IN_PROGRESS → DONE")
        void completesTask() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.HIGH, clock);
            task.start(alice, clock);
            task.pullDomainEvents();

            task.complete(laterClock);

            assertThat(task.status()).isEqualTo(TaskStatus.DONE);
            assertThat(task.completedAt()).contains(LATER);
            assertThat(task.isCompleted()).isTrue();
            assertThat(task.isBlocked()).isFalse();
        }

        @Test
        @DisplayName("IN_REVIEW → DONE")
        void completesFromInReview() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.HIGH, clock);
            task.start(alice, clock);
            task.submitForReview(clock);
            task.complete(laterClock);

            assertThat(task.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si BACKLOG")
        void throwsWhenBacklog() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.LOW, clock);

            assertThatThrownBy(() -> task.complete(clock))
                    .isInstanceOf(Task.IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("block() + reopen()")
    class BlockReopenTest {

        @Test
        @DisplayName("block() renseigne la raison sans changer le statut")
        void blockSetsReasonWithoutChangingStatus() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.MEDIUM, clock);
            task.start(alice, clock);

            task.block("Attente de validation client");

            assertThat(task.isBlocked()).isTrue();
            assertThat(task.blockedReason()).contains("Attente de validation client");
            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("block() lève IllegalTransitionException si DONE")
        void throwsBlockWhenDone() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.HIGH, clock);
            task.start(alice, clock);
            task.complete(clock);

            assertThatThrownBy(() -> task.block("Raison"))
                    .isInstanceOf(Task.IllegalTransitionException.class);
        }

        @Test
        @DisplayName("reopen() : DONE → TODO")
        void reopensTask() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.HIGH, clock);
            task.start(alice, clock);
            task.complete(clock);
            task.pullDomainEvents();

            task.reopen(laterClock);

            assertThat(task.status()).isEqualTo(TaskStatus.TODO);
            assertThat(task.completedAt()).isEmpty();
            assertThat(task.updatedAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("reopen() lève IllegalTransitionException si IN_PROGRESS")
        void throwsReopenWhenNotDone() {
            Task task = Task.create("Tâche", projectId, alice, TaskPriority.LOW, clock);
            task.start(alice, clock);

            assertThatThrownBy(() -> task.reopen(laterClock))
                    .isInstanceOf(Task.IllegalTransitionException.class);
        }
    }
}
