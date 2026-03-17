package com.uzima.domain.workspace;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.ProjectRole;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.workspace.model.TaskPriority;
import com.uzima.domain.workspace.model.TimeEntry;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Project (Aggregate Root)")
class ProjectTest {

    private static final Instant NOW   = Instant.parse("2026-03-13T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-13T10:30:00Z");

    private final TimeProvider clock      = () -> NOW;
    private final TimeProvider laterClock = () -> LATER;

    private UserId alice;
    private UserId bob;

    @BeforeEach
    void setUp() {
        alice = UserId.generate();
        bob   = UserId.generate();
    }

    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Test
        @DisplayName("crée un projet avec le créateur comme OWNER")
        void createsProjectWithOwner() {
            Project project = Project.create("Mon Projet", alice, clock);

            assertThat(project.id()).isNotNull();
            assertThat(project.name()).isEqualTo("Mon Projet");
            assertThat(project.ownerId()).isEqualTo(alice);
            assertThat(project.createdAt()).isEqualTo(NOW);
            assertThat(project.memberCount()).isEqualTo(1);
            assertThat(project.isMember(alice)).isTrue();
            assertThat(project.isManager(alice)).isTrue();
        }

        @Test
        @DisplayName("émet ProjectCreatedEvent")
        void emitsCreatedEvent() {
            Project project = Project.create("Mon Projet", alice, clock);
            List<DomainEvent> events = project.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Project.ProjectCreatedEvent.class);
            Project.ProjectCreatedEvent event = (Project.ProjectCreatedEvent) events.get(0);
            assertThat(event.ownerId()).isEqualTo(alice);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("lève InvalidProjectNameException si nom vide")
        void throwsWhenNameBlank() {
            assertThatThrownBy(() -> Project.create("  ", alice, clock))
                    .isInstanceOf(Project.InvalidProjectNameException.class);
        }

        @Test
        @DisplayName("lève InvalidProjectNameException si nom > 150 caractères")
        void throwsWhenNameTooLong() {
            String longName = "A".repeat(151);
            assertThatThrownBy(() -> Project.create(longName, alice, clock))
                    .isInstanceOf(Project.InvalidProjectNameException.class);
        }

        @Test
        @DisplayName("strip() le nom")
        void stripsName() {
            Project project = Project.create("  Mon Projet  ", alice, clock);
            assertThat(project.name()).isEqualTo("Mon Projet");
        }
    }

    @Nested
    @DisplayName("addMember()")
    class AddMemberTest {

        @Test
        @DisplayName("MANAGER+ peut ajouter un membre")
        void ownerCanAddMember() {
            Project project = Project.create("Projet", alice, clock);

            project.addMember(bob, ProjectRole.MEMBER, alice, laterClock);

            assertThat(project.memberCount()).isEqualTo(2);
            assertThat(project.isMember(bob)).isTrue();
            assertThat(project.membershipOf(bob))
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.role()).isEqualTo(ProjectRole.MEMBER));
        }

        @Test
        @DisplayName("lève DuplicateMemberException si déjà membre")
        void throwsWhenDuplicate() {
            Project project = Project.create("Projet", alice, clock);
            project.addMember(bob, ProjectRole.MEMBER, alice, clock);

            assertThatThrownBy(() -> project.addMember(bob, ProjectRole.MEMBER, alice, clock))
                    .isInstanceOf(Project.DuplicateMemberException.class);
        }

        @Test
        @DisplayName("lève IllegalArgumentException si rôle OWNER")
        void throwsWhenRoleIsOwner() {
            Project project = Project.create("Projet", alice, clock);

            assertThatThrownBy(() -> project.addMember(bob, ProjectRole.OWNER, alice, clock))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("lève InsufficientProjectPermissionException si MEMBER essaie d'ajouter")
        void throwsWhenMemberAdds() {
            Project project = Project.create("Projet", alice, clock);
            project.addMember(bob, ProjectRole.MEMBER, alice, clock);
            UserId carol = UserId.generate();

            assertThatThrownBy(() -> project.addMember(carol, ProjectRole.MEMBER, bob, clock))
                    .isInstanceOf(Project.InsufficientProjectPermissionException.class);
        }

        @Test
        @DisplayName("lève ProjectMembershipRequiredException si demandeur pas membre")
        void throwsWhenRequesterNotMember() {
            Project project = Project.create("Projet", alice, clock);

            assertThatThrownBy(() -> project.addMember(bob, ProjectRole.MEMBER, bob, clock))
                    .isInstanceOf(Project.ProjectMembershipRequiredException.class);
        }
    }

    @Nested
    @DisplayName("addTask()")
    class AddTaskTest {

        @Test
        @DisplayName("MANAGER+ peut ajouter une tâche")
        void ownerCanAddTask() {
            Project project = Project.create("Projet", alice, clock);
            Task task = Task.create("Implémenter feature", project.id(), alice, TaskPriority.HIGH, clock);

            project.addTask(task, alice);

            assertThat(project.taskCount()).isEqualTo(1);
            assertThat(project.taskIds()).contains(task.id());
        }

        @Test
        @DisplayName("lève DuplicateTaskException si tâche déjà référencée")
        void throwsWhenDuplicateTask() {
            Project project = Project.create("Projet", alice, clock);
            Task task = Task.create("Tâche", project.id(), alice, TaskPriority.LOW, clock);
            project.addTask(task, alice);

            assertThatThrownBy(() -> project.addTask(task, alice))
                    .isInstanceOf(Project.DuplicateTaskException.class);
        }

        @Test
        @DisplayName("lève InsufficientProjectPermissionException si VIEWER essaie d'ajouter")
        void throwsWhenViewerAdds() {
            Project project = Project.create("Projet", alice, clock);
            project.addMember(bob, ProjectRole.VIEWER, alice, clock);
            Task task = Task.create("Tâche", project.id(), alice, TaskPriority.LOW, clock);

            assertThatThrownBy(() -> project.addTask(task, bob))
                    .isInstanceOf(Project.InsufficientProjectPermissionException.class);
        }
    }

    @Nested
    @DisplayName("startTimeEntry()")
    class StartTimeEntryTest {

        @Test
        @DisplayName("un membre peut démarrer une entrée de temps")
        void memberCanStartTimeEntry() {
            Project project = Project.create("Projet", alice, clock);

            TimeEntry entry = project.startTimeEntry(alice, "Développement", clock);

            assertThat(entry).isNotNull();
            assertThat(entry.userId()).isEqualTo(alice);
            assertThat(entry.projectId()).isEqualTo(project.id());
            assertThat(entry.isRunning()).isTrue();
            assertThat(project.timeEntries()).hasSize(1);
        }

        @Test
        @DisplayName("lève ProjectMembershipRequiredException si pas membre")
        void throwsWhenNotMember() {
            Project project = Project.create("Projet", alice, clock);

            assertThatThrownBy(() -> project.startTimeEntry(bob, "Travail", clock))
                    .isInstanceOf(Project.ProjectMembershipRequiredException.class);
        }

        @Test
        @DisplayName("lève ActiveTimeEntryExistsException si entrée déjà en cours")
        void throwsWhenAlreadyRunning() {
            Project project = Project.create("Projet", alice, clock);
            project.startTimeEntry(alice, "Première entrée", clock);

            assertThatThrownBy(() -> project.startTimeEntry(alice, "Deuxième entrée", laterClock))
                    .isInstanceOf(Project.ActiveTimeEntryExistsException.class);
        }

        @Test
        @DisplayName("peut démarrer après avoir arrêté la précédente")
        void canStartAfterStopping() {
            Project project = Project.create("Projet", alice, clock);
            TimeEntry first = project.startTimeEntry(alice, "Première", clock);
            first.stop(clock);

            TimeEntry second = project.startTimeEntry(alice, "Deuxième", laterClock);

            assertThat(second.isRunning()).isTrue();
            assertThat(project.timeEntries()).hasSize(2);
        }
    }
}
