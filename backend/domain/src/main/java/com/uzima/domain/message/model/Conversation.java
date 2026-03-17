package com.uzima.domain.message.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate Root : Conversation.
 * Une conversation regroupe des participants et contient des messages.
 * Invariants :
 * - Au minimum 2 participants
 * - Maximum 256 participants pour un groupe
 * - Une conversation directe (DIRECT) n'a exactement que 2 participants
 * - Impossible d'ajouter deux fois le même participant
 * - Les messages ne peuvent être ajoutés que par des participants
 */
public final class Conversation {

    private static final int MAX_GROUP_SIZE = 256;
    private static final int DIRECT_CONVERSATION_SIZE = 2;

    private final ConversationId id;
    private final ConversationType type;
    private final Instant createdAt;
    private final Set<UserId> participants;

    private final String title;
    private final List<Message> recentMessages;
    private final List<DomainEvent> domainEvents;

    private Conversation(
            ConversationId id,
            ConversationType type,
            Set<UserId> participants,
            String title,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.participants = new LinkedHashSet<>(participants);
        this.title = title;
        this.recentMessages = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    /**
     * Factory method : Crée une conversation directe entre deux utilisateurs.
     */
    public static Conversation createDirect(UserId participantA, UserId participantB, TimeProvider clock) {
        Objects.requireNonNull(participantA, "Premier participant obligatoire");
        Objects.requireNonNull(participantB, "Second participant obligatoire");
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        if (participantA.equals(participantB)) {
            throw new SameParticipantException("Une conversation directe nécessite deux utilisateurs différents");
        }

        Set<UserId> participants = new LinkedHashSet<>();
        participants.add(participantA);
        participants.add(participantB);

        ConversationId id = ConversationId.generate();
        Instant now = clock.now();
        Conversation conversation = new Conversation(id, ConversationType.DIRECT, participants, null, now);
        conversation.domainEvents.add(new ConversationCreatedEvent(id, ConversationType.DIRECT, now));
        return conversation;
    }

    /**
     * Factory method : Crée une conversation de groupe.
     */
    public static Conversation createGroup(
            String title,
            Set<UserId> initialParticipants,
            TimeProvider clock
    ) {
        Objects.requireNonNull(title, "Le titre du groupe est obligatoire");
        Objects.requireNonNull(initialParticipants, "Les participants sont obligatoires");
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        if (title.isBlank()) {
            throw new InvalidConversationTitleException("Le titre du groupe ne peut pas être vide");
        }
        if (initialParticipants.size() < DIRECT_CONVERSATION_SIZE) {
            throw new InsufficientParticipantsException(
                "Un groupe nécessite au moins " + DIRECT_CONVERSATION_SIZE + " participants"
            );
        }
        if (initialParticipants.size() > MAX_GROUP_SIZE) {
            throw new GroupSizeExceededException(
                "Un groupe ne peut pas dépasser " + MAX_GROUP_SIZE + " participants"
            );
        }

        ConversationId id = ConversationId.generate();
        Instant now = clock.now();
        Conversation conversation = new Conversation(id, ConversationType.GROUP, initialParticipants, title, now);
        conversation.domainEvents.add(new ConversationCreatedEvent(id, ConversationType.GROUP, now));
        return conversation;
    }

    /**
     * Factory method : Reconstitue depuis la persistance.
     */
    public static Conversation reconstitute(
            ConversationId id,
            ConversationType type,
            Set<UserId> participants,
            String title,
            Instant createdAt
    ) {
        return new Conversation(id, type, participants, title, createdAt);
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Ajoute un participant à un groupe.
     * Interdit dans une conversation directe.
     */
    public void addParticipant(UserId newParticipant) {
        Objects.requireNonNull(newParticipant, "Le participant ne peut pas être nul");

        if (type == ConversationType.DIRECT) {
            throw new CannotAddParticipantToDirectConversationException(
                "Impossible d'ajouter un participant à une conversation directe"
            );
        }
        if (participants.contains(newParticipant)) {
            throw new ParticipantAlreadyPresentException(
                "L'utilisateur " + newParticipant + " est déjà dans la conversation"
            );
        }
        if (participants.size() >= MAX_GROUP_SIZE) {
            throw new GroupSizeExceededException(
                "Le groupe a atteint la taille maximale de " + MAX_GROUP_SIZE + " participants"
            );
        }
        participants.add(newParticipant);
    }

    /**
     * Vérifie si un utilisateur peut envoyer un message dans cette conversation.
     */
    public boolean canSendMessage(UserId userId) {
        return participants.contains(userId);
    }

    /**
     * Ajoute un message reçu (pour chargement depuis la persistance).
     */
    public void loadMessage(Message message) {
        if (!message.conversationId().equals(this.id)) {
            throw new MessageNotBelongingToConversationException(
                "Le message " + message.id() + " n'appartient pas à cette conversation"
            );
        }
        recentMessages.add(message);
    }

    /**
     * Retourne et vide les événements de domaine accumulés.
     * Garanties :
     * - Déduplique par eventId() (protection contre les doubles publications)
     * - Trie par occurredAt() pour garantir l'ordre chronologique
     * À appeler par le repository après la persistance réussie.
     *
     * @return Liste immuable d'événements, ordonnée chronologiquement
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> deduped = domainEvents.stream()
                .collect(Collectors.toMap(
                        DomainEvent::eventId,
                        e -> e,
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(DomainEvent::occurredAt))
                .toList();
        domainEvents.clear();
        return deduped;
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public ConversationId id() { return id; }
    public ConversationType type() { return type; }
    public Set<UserId> participants() { return Collections.unmodifiableSet(participants); }
    public Optional<String> title() { return Optional.ofNullable(title); }
    public Instant createdAt() { return createdAt; }
    public List<Message> recentMessages() { return Collections.unmodifiableList(recentMessages); }
    public int participantCount() { return participants.size(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation c)) return false;
        return id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // -------------------------------------------------------------------------
    // Types et exceptions
    // -------------------------------------------------------------------------

    public enum ConversationType {
        DIRECT, GROUP
    }

    /**
     * Événement de domaine : une conversation a été créée.
     * Émis par createDirect() et createGroup() (pas par reconstitute()).
     * eventId() : identifiant unique pour la déduplication lors de la publication.
     * occurredAt() : timestamp pour le tri chronologique dans pullDomainEvents().
     */
    public record ConversationCreatedEvent(
            UUID eventId,
            ConversationId conversationId,
            ConversationType conversationType,
            Instant occurredAt
    ) implements DomainEvent {
        public ConversationCreatedEvent(ConversationId conversationId, ConversationType conversationType, Instant occurredAt) {
            this(UUID.randomUUID(), conversationId, conversationType, occurredAt);
        }
    }

    public static final class SameParticipantException extends DomainException {
        public SameParticipantException(String m) { super(m); }
    }

    public static final class InvalidConversationTitleException extends DomainException {
        public InvalidConversationTitleException(String m) { super(m); }
    }

    public static final class InsufficientParticipantsException extends DomainException {
        public InsufficientParticipantsException(String m) { super(m); }
    }

    public static final class GroupSizeExceededException extends DomainException {
        public GroupSizeExceededException(String m) { super(m); }
    }

    public static final class CannotAddParticipantToDirectConversationException extends DomainException {
        public CannotAddParticipantToDirectConversationException(String m) { super(m); }
    }

    public static final class ParticipantAlreadyPresentException extends DomainException {
        public ParticipantAlreadyPresentException(String m) { super(m); }
    }

    public static final class MessageNotBelongingToConversationException extends DomainException {
        public MessageNotBelongingToConversationException(String m) { super(m); }
    }
}
