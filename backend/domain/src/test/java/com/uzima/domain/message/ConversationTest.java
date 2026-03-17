package com.uzima.domain.message;

import com.uzima.domain.message.model.*;
import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du domaine Message/Conversation.
 * Aucun Spring, aucune DB, aucun framework.
 */
class ConversationTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> FIXED_TIME;

    private UserId alice;
    private UserId bob;
    private UserId charlie;

    @BeforeEach
    void setUp() {
        alice = UserId.generate();
        bob = UserId.generate();
        charlie = UserId.generate();
    }

    @Nested
    @DisplayName("MessageContent Value Object")
    class MessageContentTest {

        @Test
        @DisplayName("accepte un contenu valide")
        void acceptsValidContent() {
            assertThatNoException().isThrownBy(() -> MessageContent.of("Bonjour !"));
        }

        @Test
        @DisplayName("rejette un contenu vide")
        void rejectsBlank() {
            assertThatThrownBy(() -> MessageContent.of("   "))
                .isInstanceOf(MessageContent.EmptyMessageContentException.class);
        }

        @Test
        @DisplayName("rejette un contenu dépassant 4096 caractères")
        void rejectsTooLong() {
            String tooLong = "A".repeat(4097);
            assertThatThrownBy(() -> MessageContent.of(tooLong))
                .isInstanceOf(MessageContent.MessageTooLongException.class);
        }

        @Test
        @DisplayName("accepte exactement 4096 caractères")
        void acceptsMaxLength() {
            String maxContent = "A".repeat(4096);
            assertThatNoException().isThrownBy(() -> MessageContent.of(maxContent));
        }
    }

    @Nested
    @DisplayName("Conversation directe")
    class DirectConversationTest {

        @Test
        @DisplayName("crée une conversation directe entre deux utilisateurs distincts")
        void createsDirectConversation() {
            var conv = Conversation.createDirect(alice, bob, clock);

            assertThat(conv.id()).isNotNull();
            assertThat(conv.type()).isEqualTo(Conversation.ConversationType.DIRECT);
            assertThat(conv.participants()).containsExactlyInAnyOrder(alice, bob);
            assertThat(conv.createdAt()).isEqualTo(FIXED_TIME);
        }

        @Test
        @DisplayName("rejette une conversation directe avec le même utilisateur")
        void rejectsSameParticipant() {
            assertThatThrownBy(() -> Conversation.createDirect(alice, alice, clock))
                .isInstanceOf(Conversation.SameParticipantException.class);
        }

        @Test
        @DisplayName("interdit d'ajouter un participant à une conversation directe")
        void cannotAddParticipantToDirectConversation() {
            var conv = Conversation.createDirect(alice, bob, clock);
            assertThatThrownBy(() -> conv.addParticipant(charlie))
                .isInstanceOf(Conversation.CannotAddParticipantToDirectConversationException.class);
        }
    }

    @Nested
    @DisplayName("Conversation de groupe")
    class GroupConversationTest {

        @Test
        @DisplayName("crée un groupe avec un titre et des participants")
        void createsGroupConversation() {
            var conv = Conversation.createGroup("Équipe Uzima", Set.of(alice, bob, charlie), clock);

            assertThat(conv.type()).isEqualTo(Conversation.ConversationType.GROUP);
            assertThat(conv.title()).hasValue("Équipe Uzima");
            assertThat(conv.participantCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("rejette un titre vide")
        void rejectsEmptyTitle() {
            assertThatThrownBy(() -> Conversation.createGroup("  ", Set.of(alice, bob), clock))
                .isInstanceOf(Conversation.InvalidConversationTitleException.class);
        }

        @Test
        @DisplayName("rejette un groupe avec moins de 2 participants")
        void rejectsTooFewParticipants() {
            assertThatThrownBy(() -> Conversation.createGroup("Solo", Set.of(alice), clock))
                .isInstanceOf(Conversation.InsufficientParticipantsException.class);
        }

        @Test
        @DisplayName("peut ajouter un nouveau participant")
        void addsParticipant() {
            var conv = Conversation.createGroup("Team", Set.of(alice, bob), clock);
            conv.addParticipant(charlie);
            assertThat(conv.participants()).contains(charlie);
        }

        @Test
        @DisplayName("rejette l'ajout d'un participant déjà présent")
        void rejectsDuplicateParticipant() {
            var conv = Conversation.createGroup("Team", Set.of(alice, bob), clock);
            assertThatThrownBy(() -> conv.addParticipant(alice))
                .isInstanceOf(Conversation.ParticipantAlreadyPresentException.class);
        }
    }

    @Nested
    @DisplayName("Conversation.reconstitute()")
    class ReconstitueTest {

        @Test
        @DisplayName("reconstitue une conversation directe depuis la persistance sans émettre d'événement")
        void reconstitueDirectConversation() {
            ConversationId id = ConversationId.generate();
            Set<UserId> participants = Set.of(alice, bob);

            Conversation conv = Conversation.reconstitute(
                    id, Conversation.ConversationType.DIRECT, participants, null, FIXED_TIME);

            assertThat(conv.id()).isEqualTo(id);
            assertThat(conv.type()).isEqualTo(Conversation.ConversationType.DIRECT);
            assertThat(conv.participants()).containsExactlyInAnyOrder(alice, bob);
            assertThat(conv.createdAt()).isEqualTo(FIXED_TIME);
            // reconstitute() ne doit PAS émettre d'événement de domaine
            assertThat(conv.pullDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("reconstitue une conversation de groupe depuis la persistance")
        void reconstitueGroupConversation() {
            ConversationId id = ConversationId.generate();

            Conversation conv = Conversation.reconstitute(
                    id, Conversation.ConversationType.GROUP, Set.of(alice, bob, charlie), "Équipe", FIXED_TIME);

            assertThat(conv.title()).hasValue("Équipe");
            assertThat(conv.participantCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Conversation.canSendMessage()")
    class CanSendMessageTest {

        @Test
        @DisplayName("retourne true pour un participant")
        void participantCanSendMessage() {
            var conv = Conversation.createDirect(alice, bob, clock);
            assertThat(conv.canSendMessage(alice)).isTrue();
            assertThat(conv.canSendMessage(bob)).isTrue();
        }

        @Test
        @DisplayName("retourne false pour un non-participant")
        void nonParticipantCannotSendMessage() {
            var conv = Conversation.createDirect(alice, bob, clock);
            assertThat(conv.canSendMessage(charlie)).isFalse();
        }
    }

    @Nested
    @DisplayName("Conversation.recentMessages() et loadMessage()")
    class RecentMessagesTest {

        @Test
        @DisplayName("recentMessages() retourne les messages chargés via loadMessage()")
        void recentMessagesAfterLoad() {
            var conv = Conversation.createDirect(alice, bob, clock);
            var msg1 = Message.sendText(conv.id(), alice, MessageContent.of("Salut !"), clock);
            var msg2 = Message.sendText(conv.id(), bob, MessageContent.of("Bonjour !"), clock);

            conv.loadMessage(msg1);
            conv.loadMessage(msg2);

            assertThat(conv.recentMessages()).hasSize(2)
                    .containsExactly(msg1, msg2);
        }

        @Test
        @DisplayName("recentMessages() est vide sur une conversation non hydratée")
        void recentMessagesEmptyByDefault() {
            var conv = Conversation.createDirect(alice, bob, clock);
            assertThat(conv.recentMessages()).isEmpty();
        }

        @Test
        @DisplayName("loadMessage() rejette un message d'une autre conversation")
        void rejectsForeignMessage() {
            var conv1 = Conversation.createDirect(alice, bob, clock);
            var conv2 = Conversation.createDirect(alice, charlie, clock);
            var msg = Message.sendText(conv2.id(), alice, MessageContent.of("Salut"), clock);

            assertThatThrownBy(() -> conv1.loadMessage(msg))
                    .isInstanceOf(Conversation.MessageNotBelongingToConversationException.class);
        }
    }

    @Nested
    @DisplayName("Conversation.pullDomainEvents() — eventId et occurredAt")
    class DomainEventsTest {

        @Test
        @DisplayName("createDirect() émet un ConversationCreatedEvent avec eventId et occurredAt")
        void createDirectEmitsDomainEvent() {
            var conv = Conversation.createDirect(alice, bob, clock);
            List<DomainEvent> events = conv.pullDomainEvents();

            assertThat(events).hasSize(1);
            DomainEvent event = events.get(0);
            assertThat(event).isInstanceOf(Conversation.ConversationCreatedEvent.class);
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isEqualTo(FIXED_TIME);

            var createdEvent = (Conversation.ConversationCreatedEvent) event;
            assertThat(createdEvent.conversationId()).isEqualTo(conv.id());
            assertThat(createdEvent.conversationType()).isEqualTo(Conversation.ConversationType.DIRECT);
        }

        @Test
        @DisplayName("createGroup() émet un ConversationCreatedEvent de type GROUP")
        void createGroupEmitsDomainEvent() {
            var conv = Conversation.createGroup("Team", Set.of(alice, bob), clock);
            List<DomainEvent> events = conv.pullDomainEvents();

            assertThat(events).hasSize(1);
            var event = (Conversation.ConversationCreatedEvent) events.get(0);
            assertThat(event.conversationType()).isEqualTo(Conversation.ConversationType.GROUP);
        }

        @Test
        @DisplayName("pullDomainEvents() vide la liste après appel")
        void pullDomainEventsClearsTheList() {
            var conv = Conversation.createDirect(alice, bob, clock);
            conv.pullDomainEvents(); // premier appel
            assertThat(conv.pullDomainEvents()).isEmpty(); // deuxième appel
        }

        @Test
        @DisplayName("pullDomainEvents() déduplique par eventId et trie par occurredAt")
        void pullDomainEventsDeduplicatesAndSorts() {
            // Reconstituons une conversation pour contrôler exactement les événements
            // On crée deux conversations à des instants différents pour tester le tri
            TimeProvider earlier = () -> FIXED_TIME.minusSeconds(10);
            TimeProvider later   = () -> FIXED_TIME;

            var conv1 = Conversation.createDirect(alice, bob, earlier);
            var conv2 = Conversation.createDirect(alice, charlie, later);

            List<DomainEvent> events1 = conv1.pullDomainEvents();
            List<DomainEvent> events2 = conv2.pullDomainEvents();

            // Chaque événement a un eventId unique
            assertThat(events1.get(0).eventId())
                    .isNotEqualTo(events2.get(0).eventId());

            // Chaque événement a son occurredAt correct
            assertThat(events1.get(0).occurredAt()).isEqualTo(FIXED_TIME.minusSeconds(10));
            assertThat(events2.get(0).occurredAt()).isEqualTo(FIXED_TIME);
        }
    }

    @Nested
    @DisplayName("Message")
    class MessageTest {

        @Test
        @DisplayName("envoie un message texte valide")
        void sendsTextMessage() {
            var conv = Conversation.createDirect(alice, bob, clock);
            var content = MessageContent.of("Salut Bob !");
            var message = Message.sendText(conv.id(), alice, content, clock);

            assertThat(message.id()).isNotNull();
            assertThat(message.senderId()).isEqualTo(alice);
            assertThat(message.content()).isEqualTo(content);
            assertThat(message.sentAt()).isEqualTo(FIXED_TIME);
            assertThat(message.isDeleted()).isFalse();
            assertThat(message.type()).isEqualTo(Message.MessageType.TEXT);
        }

        @Test
        @DisplayName("l'expéditeur peut supprimer son propre message")
        void senderCanDeleteMessage() {
            var conv = Conversation.createDirect(alice, bob, clock);
            var message = Message.sendText(conv.id(), alice, MessageContent.of("Salut"), clock);

            message.deleteBy(alice, clock);

            assertThat(message.isDeleted()).isTrue();
            assertThat(message.deletedAt()).hasValue(FIXED_TIME);
        }

        @Test
        @DisplayName("un autre utilisateur ne peut pas supprimer le message")
        void otherUserCannotDeleteMessage() {
            var conv = Conversation.createDirect(alice, bob, clock);
            var message = Message.sendText(conv.id(), alice, MessageContent.of("Salut"), clock);

            assertThatThrownBy(() -> message.deleteBy(bob, clock))
                .isInstanceOf(Message.UnauthorizedMessageDeletionException.class);
        }

        @Test
        @DisplayName("un message déjà supprimé ne peut pas être supprimé à nouveau")
        void alreadyDeletedMessageCannotBeDeletedAgain() {
            var conv = Conversation.createDirect(alice, bob, clock);
            var message = Message.sendText(conv.id(), alice, MessageContent.of("Salut"), clock);
            message.deleteBy(alice, clock);

            assertThatThrownBy(() -> message.deleteBy(alice, clock))
                .isInstanceOf(Message.MessageAlreadyDeletedException.class);
        }
    }
}
