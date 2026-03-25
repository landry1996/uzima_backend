# 📂 PREUVES CODE DÉTAILLÉES - UZIMA BACKEND

## Fichiers Analysés : 147 fichiers Java

---

## 1️⃣ QR CODE - PREUVES IMPLÉMENTATION

### ✅ Domain Model


#### QrCodeType.java (Types contextuels)
package com.uzima.domain.qrcode.model;

/**
 * Type de QR Code contextuel (Innovation Brevetable #1 du cahier des charges).
 *
 * Chaque type porte sa propre sémantique quant aux données exposées
 * et au comportement lors du scan.
 */
public enum QrCodeType {

    /** Réseau pro, CV, portfolio, prise de RDV */
    PROFESSIONAL("Professionnel"),

    /** Profil social, centres d'intérêt, ajout cercle amis */
    SOCIAL("Social"),

    /** Uniquement coordonnées paiement (données minimales, sécurité) */
    PAYMENT("Paiement"),

    /** Position GPS temporaire, durée limitée, auto-destruction */
    TEMPORARY_LOCATION("Localisation temporaire"),

    /** Badge événement, réseau privé temporaire, expire après l'événement */
    EVENT("Événement"),

    /** Groupe sanguin, allergies, contacts d'urgence - accessible offline */
    MEDICAL_EMERGENCY("Urgence médicale");

    private final String displayName;

    QrCodeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Règle métier : le QR code médical d'urgence doit pouvoir fonctionner
     * sans connexion internet (données encodées dans le QR lui-même).
     */
    public boolean requiresOfflineSupport() {
        return this == MEDICAL_EMERGENCY;
    }

    /**
     * Règle métier : certains types ont une expiration obligatoire.
     */
    public boolean expirationIsMandatory() {
        return this == TEMPORARY_LOCATION || this == EVENT;
    }
}


#### QrCode.java (Aggregate Root - extrait)
package com.uzima.domain.qrcode.model;

import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root : QR Code Contextuel Intelligent (Innovation Brevetable #1).
 *
 * Un QR Code appartient à un utilisateur, possède un type contextuel,
 * une politique d'expiration et une limite de scans.
 *
 * Invariants :
 * - L'owner est toujours défini
 * - Le type est toujours défini
 * - Les types à expiration obligatoire (TEMPORARY_LOCATION, EVENT) doivent avoir une date d'expiration
 * - Un QR révoqué ne peut plus être scanné
 * - Le compteur de scans ne peut pas dépasser la limite
 * - Un QR expiré ne peut pas être scanné
 */
public final class QrCode {

    private final QrCodeId id;
    private final UserId ownerId;
    private final QrCodeType type;
    private final ExpirationPolicy expirationPolicy;
    private final ScanLimit scanLimit;
    private final Instant createdAt;

    private int scanCount;
    private boolean revoked;
    private Instant revokedAt;

    private QrCode(
            QrCodeId id,
            UserId ownerId,
            QrCodeType type,
            ExpirationPolicy expirationPolicy,
            ScanLimit scanLimit,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.type = Objects.requireNonNull(type);
        this.expirationPolicy = Objects.requireNonNull(expirationPolicy);
        this.scanLimit = Objects.requireNonNull(scanLimit);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.scanCount = 0;
        this.revoked = false;
        this.revokedAt = null;
    }

    /**
     * Factory method : Crée un nouveau QR Code.
     *
     * Applique la règle métier : certains types exigent une expiration.
     */
    public static QrCode create(
            UserId ownerId,
            QrCodeType type,
            ExpirationPolicy expirationPolicy,
            ScanLimit scanLimit,
            TimeProvider clock
    ) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        // Règle métier : TEMPORARY_LOCATION et EVENT doivent avoir une expiration
        if (type.expirationIsMandatory() && expirationPolicy.isPermanent()) {
            throw new ExpirationRequiredForTypeException(
                "Le type " + type.displayName() + " requiert une date d'expiration"
            );
        }

        return new QrCode(
                QrCodeId.generate(),
                ownerId,
                type,


### ✅ Use Cases

#### CreateQrCodeUseCase.java
package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.in.CreateQrCodeCommand;
import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.domain.qrcode.factory.QrCodeFactory;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Création d'un QR Code contextuel.
 *
 * Justifie l'existence de :
 * - QrCodeFactory.createSocial()
 * - QrCodeFactory.createEvent()
 * - QrCodeRepository (et QrCodeRepositoryPort)
 *
 * La factory est le seul point de création : les règles par type
 * (expiration obligatoire pour EVENT, illimité pour SOCIAL, etc.)
 * sont encapsulées dans QrCodeFactory et ne fuient pas dans ce use case.
 *
 * Pas de Spring. Pas de framework.
 */
public final class CreateQrCodeUseCase {

    private final QrCodeRepositoryPort qrCodeRepository;
    private final TimeProvider clock;

    public CreateQrCodeUseCase(QrCodeRepositoryPort qrCodeRepository, TimeProvider clock) {
        this.qrCodeRepository = Objects.requireNonNull(qrCodeRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Crée et persiste un QR Code selon le type demandé.
     *
     * @param command La commande contenant le type et les paramètres optionnels
     * @return Le QR Code créé
     * @throws IllegalArgumentException si les paramètres sont invalides pour le type
     */
    public QrCode execute(CreateQrCodeCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        QrCode qrCode = switch (command.type()) {
            case PROFESSIONAL -> QrCodeFactory.createProfessional(command.ownerId(), clock);
            // createSocial justifié ici — Nouveauté QR Code social du super-app
            case SOCIAL -> QrCodeFactory.createSocial(command.ownerId(), clock);
            case PAYMENT -> QrCodeFactory.createPayment(command.ownerId(), command.singleUsePayment(), clock);
            case TEMPORARY_LOCATION -> {


---

## 2️⃣ MESSAGERIE - PREUVES IMPLÉMENTATION

### ✅ Domain Model

#### Message.java (Aggregate Root - extrait)
package com.uzima.domain.message.model;

import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root : Message.
 * Un message est immuable une fois envoyé (son contenu ne change pas).
 * Il peut être marqué comme supprimé (soft delete).
 * Invariants :
 * - L'expéditeur (senderId) est toujours défini
 * - La conversation est toujours définie
 * - Le contenu est toujours valide (validé par MessageContent VO)
 * - La date d'envoi est toujours définie
 */
public final class Message {

    private final MessageId id;
    private final ConversationId conversationId;
    private final UserId senderId;
    private final MessageContent content;
    private final MessageType type;
    private final Instant sentAt;

    // Seul état mutable
    private boolean deleted;
    private Instant deletedAt;

    private Message(
            MessageId id,
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            MessageType type,
            Instant sentAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.senderId = Objects.requireNonNull(senderId);
        this.content = Objects.requireNonNull(content);
        this.type = Objects.requireNonNull(type);
        this.sentAt = Objects.requireNonNull(sentAt);
        this.deleted = false;
        this.deletedAt = null;
    }

    /**
     * Factory method : Envoie un nouveau message texte.
     */
    public static Message sendText(
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            TimeProvider clock
    ) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");
        return new Message(
                MessageId.generate(),
                conversationId,
                senderId,
                content,
                MessageType.TEXT,
                clock.now()
        );
    }

    /**
     * Factory method : Reconstitue un message depuis la persistance.
     */
    public static Message reconstitute(
            MessageId id,
            ConversationId conversationId,
            UserId senderId,
            MessageContent content,
            MessageType type,
            Instant sentAt,


#### MessageType enum
    public enum MessageType {
        TEXT, VOICE, VIDEO, IMAGE, DOCUMENT, PAYMENT_REQUEST, LOCATION
    }

    public static final class MessageAlreadyDeletedException extends DomainException {
        public MessageAlreadyDeletedException(String message) { super(message); }
    }

    public static final class UnauthorizedMessageDeletionException extends DomainException {
        public UnauthorizedMessageDeletionException(String message) { super(message); }
    }


### ✅ Use Cases

#### SendMessageUseCase.java
package com.uzima.application.message;

import com.uzima.application.message.port.in.SendMessageCommand;
import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.MessageNotificationPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageContent;
import com.uzima.domain.message.specification.SenderIsParticipantSpecification;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use Case : Envoi d'un message dans une conversation.
 *
 * Changements vs version initiale :
 * 1. Utilise SenderIsParticipantSpecification — Justifie la Specification
 * 2. Vérifie allowsVoiceMessages() pour les messages vocaux — Justifie la méthode PresenceStatus
 *
 * Orchestration :
 * 1. Charger la conversation
 * 2. Vérifier que l'expéditeur est participant (via Specification)
 * 3. Pour les messages vocaux : vérifier que les destinataires l'acceptent
 * 4. Créer le message (logique domaine)
 * 5. Persister
 * 6. Notifier les autres participants
 */
public final class SendMessageUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final MessageRepositoryPort messageRepository;
    private final MessageNotificationPort notificationPort;
    private final UserRepositoryPort userRepository;
    private final TimeProvider clock;

    public SendMessageUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository,
            MessageNotificationPort notificationPort,
            UserRepositoryPort userRepository,
            TimeProvider clock


---

## 3️⃣ USER & PRESENCE - PREUVES IMPLÉMENTATION

### ✅ PresenceStatus.java (États enrichis)
package com.uzima.domain.user.model;

/**
 * Enumération des états de présence d'un utilisateur.
 * Chaque état porte sa propre sémantique métier (Nouveauté 6 du cahier des charges).
 * - AVAILABLE     : Disponible, notifications normales
 * - FOCUSED       : Concentré (Deep Work), notifications différées sauf urgences
 * - TIRED         : Fatigué, suggère messages courts, pas de vocaux longs
 * - TRAVELING     : En déplacement, réponses vocales uniquement
 * - SILENCE       : Besoin de silence, mode texte uniquement, pas d'appels
 * - PHYSICAL_ACTIVITY : Activité physique, urgences uniquement
 * - WELLNESS      : Bien-être / méditation, notifications bloquées 100%
 * - SLEEPING      : Sommeil, urgences filtrées (famille seulement)
 * - CELEBRATING   : Mode festif, suggestions réponses joyeuses
 * - OFFLINE       : Hors ligne
 */
public enum PresenceStatus {

    AVAILABLE("Disponible", NotificationPolicy.NORMAL),
    FOCUSED("Concentré", NotificationPolicy.DEFERRED),
    TIRED("Fatigué", NotificationPolicy.DEFERRED),
    TRAVELING("En déplacement", NotificationPolicy.URGENT_ONLY),
    SILENCE("Silence", NotificationPolicy.DEFERRED),
    PHYSICAL_ACTIVITY("Activité physique", NotificationPolicy.URGENT_ONLY),
    WELLNESS("Bien-être", NotificationPolicy.BLOCKED),
    SLEEPING("Sommeil", NotificationPolicy.URGENT_ONLY),
    CELEBRATING("Célébration", NotificationPolicy.NORMAL),
    OFFLINE("Hors ligne", NotificationPolicy.BLOCKED);

    private final String displayName;
    private final NotificationPolicy notificationPolicy;

    PresenceStatus(String displayName, NotificationPolicy notificationPolicy) {
        this.displayName = displayName;
        this.notificationPolicy = notificationPolicy;
    }

    public String displayName() {
        return displayName;
    }

    public NotificationPolicy notificationPolicy() {
        return notificationPolicy;
    }

    public boolean allowsVoiceMessages() {
        return this != SILENCE && this != WELLNESS && this != SLEEPING;
    }

    public boolean allowsPhoneCalls() {
        return this == AVAILABLE || this == CELEBRATING;
    }

    /**
     * Politique de notification selon l'état de présence.
     * Utilisée par l'infrastructure pour décider comment router les notifications.
     */
    public enum NotificationPolicy {
        /** Notifications immédiates normales */
        NORMAL,
        /** Notifications différées (batching) */
        DEFERRED,
        /** Uniquement les urgences (famille, contacts prioritaires) */
        URGENT_ONLY,
        /** Aucune notification */
        BLOCKED
    }
}


### ✅ Notification Strategies


#### BlockedNotificationStrategy.java
package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Stratégie : Notification BLOQUÉE.
 *
 * Appliquée pour : WELLNESS, OFFLINE (NotificationPolicy.BLOCKED)
 *
 * Comportement :
 * - Aucune notification temps réel
 * - Stockage en base (le message reste disponible quand l'utilisateur revient)
 * - En production : la notification sera visible au prochain login/retour en ligne
 *
 * Noteworthy : même BLOCKED stocke le message — l'utilisateur ne perd rien,
 * il verra ses messages non lus à son retour.
 */
public final class BlockedNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = Logger.getLogger(BlockedNotificationStrategy.class.getName());

    @Override
    public void route(PendingNotification notification) {
        Objects.requireNonNull(notification);
        // Message déjà persisté en DB par MessageRepositoryPort.save()

#### DeferredNotificationStrategy.java
package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Stratégie : Notification DIFFÉRÉE (batching).
 *
 * Appliquée pour : FOCUSED, TIRED, SILENCE (NotificationPolicy.DEFERRED)
 *
 * Comportement :
 * - Stocke la notification dans une file en mémoire (ConcurrentLinkedQueue)
 *   indexée par queueKey(notification) → "notifications:deferred:{recipientId}"
 * - Livre lors de la prochaine fenêtre batch via drainQueue(recipientId)
 *   (appelé quand l'état de présence repasse à AVAILABLE)
 *
 * Résultat visible par l'expéditeur (Nouveauté 7) :
 * "Marie est concentrée, message livré silencieusement"
 *
 * Note : Pour la production, remplacer la ConcurrentLinkedQueue par un
 * RedisTemplate<String, PendingNotification> en conservant la même interface.

#### ImmediateNotificationStrategy.java
package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Stratégie : Notification IMMÉDIATE.
 *
 * Appliquée pour : AVAILABLE, CELEBRATING (NotificationPolicy.NORMAL)
 * Comportement : envoie la notification en temps réel via WebSocket/Socket.IO
 *
 * TODO : Injecter SocketIOServer ou SimpMessagingTemplate
 *        pour l'envoi WebSocket réel en production.
 */
public final class ImmediateNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = Logger.getLogger(ImmediateNotificationStrategy.class.getName());

    // TODO : Injecter le client WebSocket réel (SocketIOServer, etc.)

    @Override
    public void route(PendingNotification notification) {
        Objects.requireNonNull(notification);
        // En production : websocketClient.sendToUser(notification.recipientId(), notification.message())
        log.info("[IMMEDIATE] Notification envoyée à " + notification.recipientId()
                + " pour message " + notification.message().id());

#### PresenceAwareNotificationAdapter.java
package com.uzima.infrastructure.notification;

import com.uzima.application.message.port.out.MessageNotificationPort;
import com.uzima.application.notification.NotificationRouter;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.PresenceStatus;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Adaptateur : Implémente MessageNotificationPort avec routing basé sur PresenceStatus.
 *
 * Remplace le stub vide dans InfrastructureConfiguration.
 *
 * Principe (Nouveauté 7 - "Respect Automatique États") :
 * 1. Pour chaque destinataire, récupère son PresenceStatus
 * 2. Délègue au NotificationRouter qui sélectionne la bonne Strategy
 * 3. La stratégie choisie livre la notification de façon appropriée
 *
 * L'application ne connaît que MessageNotificationPort.
 * L'infrastructure gère le routing par présence — transparence totale.
 */
public final class PresenceAwareNotificationAdapter implements MessageNotificationPort {

    private static final Logger log = Logger.getLogger(PresenceAwareNotificationAdapter.class.getName());

#### UrgentOnlyNotificationStrategy.java
package com.uzima.infrastructure.notification;

import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.notification.PendingNotification;
import com.uzima.domain.user.model.PresenceStatus;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Stratégie : Notification pour URGENCES UNIQUEMENT.
 *
 * Appliquée pour : TRAVELING, PHYSICAL_ACTIVITY, SLEEPING (NotificationPolicy.URGENT_ONLY)
 *
 * Comportement :
 * - Si la notification est urgente → envoi immédiat
 * - Si la notification est normale → mise en file différée
 *
 * En production, "urgente" = contact prioritaire (famille, contacts marqués "urgence").
 * Cette logique de priorité est dans l'application layer (hors scope ici).
 */
public final class UrgentOnlyNotificationStrategy implements NotificationRoutingStrategy {

    private static final Logger log = Logger.getLogger(UrgentOnlyNotificationStrategy.class.getName());

    private final ImmediateNotificationStrategy immediateStrategy;
    private final DeferredNotificationStrategy deferredStrategy;

    public UrgentOnlyNotificationStrategy(
            ImmediateNotificationStrategy immediateStrategy,


---

## 4️⃣ ARCHITECTURE - STRUCTURE MODULES


### Modules Maven

```
application
bootstrap
domain
infrastructure
security
```

### domain/pom.xml (Aucune dépendance externe)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.uzima</groupId>
        <artifactId>uzima-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>uzima-domain</artifactId>
    <name>Uzima Domain - Pure Business Logic (No Frameworks)</name>

    <!--
        RÈGLE ABSOLUE : Ce module NE DOIT avoir AUCUNE dépendance externe.
        Ni Spring, ni JPA, ni Lombok.
        Uniquement le JDK standard.
        Ce module doit compiler et fonctionner seul.
    -->
    <dependencies>
        <!-- AUCUNE dépendance externe - uniquement JDK standard -->

        <!-- Test only -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```


---

## 5️⃣ TESTS - PREUVES QUALITÉ


### Tests Domain (sans Spring)


#### UserTest.java
package com.uzima.domain.user;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du domaine User.
 *
 * Ces tests s'exécutent sans Spring, sans base de données, sans aucun framework.
 * Le TimeProvider est un stub déterministe.
 */
class UserTest {

    // Stub déterministe du TimeProvider (pas de Instant.now() dans les tests)
    private static final Instant FIXED_TIME = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> FIXED_TIME;

    private PhoneNumber validPhone;
    private CountryCode validCountry;
    private FirstName validFirstName;
    private LastName validLastName;
    private String validHash;

    @BeforeEach
    void setUp() {
        validPhone = PhoneNumber.of("+33612345678");
        validCountry = CountryCode.of("FR");
        validFirstName = FirstName.of("Alice");
        validLastName = LastName.of("Dupont");
        validHash = "$2a$10$hashedPasswordExample";
    }


#### QrCodeTest.java
package com.uzima.domain.qrcode;

import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.qrcode.specification.*;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du domaine QrCode.
 * Aucun Spring, aucune DB, aucun framework.
 */
class QrCodeTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private static final Instant PAST = NOW.minus(Duration.ofHours(1));
    private static final Instant FUTURE = NOW.plus(Duration.ofHours(24));

    private final TimeProvider clock = () -> NOW;
    private UserId owner;

    @BeforeEach
    void setUp() {
        owner = UserId.generate();
    }

    @Nested
    @DisplayName("QrCode.reconstitute() et createdAt()")
    class ReconstitueTest {

        @Test
        @DisplayName("reconstitue un QR Code depuis la persistance avec les bonnes valeurs")

#### QrCodeFactoryTest.java
package com.uzima.domain.qrcode;

import com.uzima.domain.qrcode.factory.QrCodeFactory;
import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de QrCodeFactory.
 * Vérifie que la factory applique correctement les règles par type.
 */
class QrCodeFactoryTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> NOW;
    private UserId owner;

    @BeforeEach
    void setUp() {
        owner = UserId.generate();
    }

    @Test
    @DisplayName("createProfessional : permanent, illimité")
    void professionalIsPermanentAndUnlimited() {
        QrCode qr = QrCodeFactory.createProfessional(owner, clock);
        assertThat(qr.type()).isEqualTo(QrCodeType.PROFESSIONAL);
        assertThat(qr.expirationPolicy().isPermanent()).isTrue();
        assertThat(qr.scanLimit().isUnlimited()).isTrue();
        assertThat(qr.isActiveAt(NOW)).isTrue();
    }

#### QrCodeDomainServiceTest.java
package com.uzima.domain.qrcode;

import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.qrcode.port.QrCodeRepository;
import com.uzima.domain.qrcode.service.QrCodeDomainService;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du service de domaine QrCodeDomainService.
 *
 * Appelle et vérifie :
 * - QrCodeDomainService.deleteIfOwned()        → QrCodeRepository.delete() + ownership check
 * - QrCodeDomainService.getLifecycleSummary()  → QrCode.createdAt() + ExpirationPolicy.expiresAt()
 * - LifecycleSummary.hasExpiration()           → Optional.isPresent() sur expiresAt()
 *
 * Pas de Mockito : QrCodeRepository implémenté in-memory pour rester dans les règles du module.
 */
class QrCodeDomainServiceTest {

    private static final Instant NOW    = Instant.parse("2026-03-12T10:00:00Z");
    private static final Instant FUTURE = NOW.plus(Duration.ofHours(48));

    private final TimeProvider clock = () -> NOW;

    private UserId owner;
    private UserId otherUser;
    private InMemoryQrCodeRepository repository;
    private QrCodeDomainService service;


#### AccountLockoutPolicyTest.java
package com.uzima.domain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires de la politique de verrouillage.
 * Aucun Spring, aucune DB. TimeProvider stubbé.
 */
class AccountLockoutPolicyTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private static final String IDENTIFIER = "+33612345678";

    // Politique avec seuils réduits pour faciliter les tests
    private AccountLockoutPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AccountLockoutPolicy(
                3, 5, 8,                              // seuils (doux/moyen/dur)
                Duration.ofMinutes(5),                 // fenêtre doux
                Duration.ofMinutes(30),                // fenêtre moyen
                Duration.ofHours(1),                   // fenêtre dur
                Duration.ofMinutes(1),                 // lock doux
                Duration.ofMinutes(5),                 // lock moyen
                Duration.ofMinutes(30)                 // lock dur
        );
    }

    @Nested

#### ConversationTest.java
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
