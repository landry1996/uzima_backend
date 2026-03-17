# Architecture Technique — Uzima Backend

## Sommaire

- [Vue d'ensemble](#vue-densemble)
- [Principes fondamentaux](#principes-fondamentaux)
- [Modules Maven](#modules-maven)
- [DDD — Domain-Driven Design](#ddd--domain-driven-design)
- [Hexagonal — Ports & Adapters](#hexagonal--ports--adapters)
- [Flux d'une requête HTTP](#flux-dune-requête-http)
- [Sécurité & Auth](#sécurité--auth)
- [Gestion des notifications](#gestion-des-notifications)
- [Stratégie IA (MVP)](#stratégie-ia-mvp)
- [Base de données](#base-de-données)
- [Conventions et règles](#conventions-et-règles)
- [Décisions d'architecture](#décisions-darchitecture)

---

## Vue d'ensemble

Uzima backend implémente **DDD (Domain-Driven Design) + Clean Architecture + Architecture Hexagonale** en Java 25 / Spring Boot 3.5.

```
┌──────────────────────────────────────────────────────────────────┐
│                          bootstrap                               │
│   Spring Boot App · REST Controllers · Spring Security           │
│   DomainConfiguration · InfrastructureConfiguration             │
│   GlobalExceptionHandler · RateLimitingFilter                    │
├──────────────────────────────────────────────────────────────────┤
│                        infrastructure                            │
│   JPA Entities · Spring Data Repos · Entity Mappers              │
│   Stub AI Adapters · Notification Strategies                     │
│   BCrypt · LibPhoneNumber · JWT Impl                             │
├─────────────────────────────┬────────────────────────────────────┤
│         application         │             security               │
│   Use Cases (52)            │   JwtTokenService                  │
│   Commands / Queries        │   AccessTokenVerifierPort          │
│   Ports in / out            │   RefreshTokenHasherPort           │
│   NotificationRouter        │   Token Family Pattern             │
│   BruteForceProtectionService│                                   │
├─────────────────────────────┴────────────────────────────────────┤
│                           domain                                 │
│   Aggregates · Value Objects · Domain Services                   │
│   Domain Events · Specifications · Ports (interfaces)           │
│   0 dépendances externes — JDK seul                             │
└──────────────────────────────────────────────────────────────────┘
         Règle de dépendance : chaque couche dépend UNIQUEMENT
         des couches inférieures (jamais vers le haut)
```

---

## Principes fondamentaux

### 1. Isolation totale du domaine

Le module `domain` ne contient **aucune** annotation Spring, JPA ou Lombok. Il compile et teste avec le JDK seul.

```
domain/pom.xml :
  <dependencies>
    <!-- AUCUNE dépendance externe -->
    <!-- Test only : JUnit5 + AssertJ -->
  </dependencies>
```

### 2. TimeProvider — testabilité déterministe

Aucun `Instant.now()` n'est autorisé dans `domain/` ou `application/`. Toute référence au temps passe par `TimeProvider`.

```java
// domain/shared/TimeProvider.java
@FunctionalInterface
public interface TimeProvider {
    Instant now();
}

// En production (bootstrap)
@Bean
public TimeProvider timeProvider() {
    return Instant::now;  // seul endroit autorisé
}

// En test
TimeProvider clock = () -> Instant.parse("2026-03-13T10:00:00Z");
```

### 3. Factory methods + constructeurs privés

```java
// Correct
public final class QrCode {
    private QrCode(...) { ... }                    // privé
    public static QrCode create(...) { ... }       // factory
    public static QrCode reconstitute(...) { ... } // hydratation DB
}

// Interdit
@Builder  // ❌
public class QrCode { ... }
```

### 4. Value Objects = records Java

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        // Validation dans le compact constructor
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new NegativeAmountException(amount);
    }
}
```

---

## Modules Maven

### Graphe de dépendances

```
bootstrap ──────────────────────────────────────────┐
    │                                                │
    ├── infrastructure ──┐                           │
    │       │            │                           │
    │       ├── application ──── security            │
    │       │       │                                │
    │       └── domain ◄──────────────────────────-─┘
    │
    └── (tous les modules ci-dessus)
```

### `uzima-domain`

**Rôle** : Logique métier pure. Aucune dépendance externe.

Contenu :
- **Aggregates** (classes `final`, constructeurs privés) : `User`, `Message`, `QrCode`, `Transaction`, `Circle`, `Project`, `Task`, `TimeEntry`, `Invoice`, `InvoiceItem`, `Reminder`, `FocusSession`, `UsageSession`
- **Value Objects** (records) : `Money`, `MessageMetadata`, `GeofenceRule`, `PersonalizationRule`, `DigitalHealthMetrics`, `WellbeingReport`, `CircleRule`, `TaxRate`…
- **Enums** : `QrCodeType`, `MessageType`, `PresenceStatus`, `TransactionStatus`, `ReminderStatus`…
- **Domain Events** (records implements `DomainEvent`) : `TransactionCompletedEvent`, `ReminderTriggeredEvent`, `FocusSessionEndedEvent`
- **Domain Services** : `AccountLockoutPolicy`
- **Ports** (interfaces — définis dans domain, implémentés dans infrastructure)
- **Specifications** : `Specification<T>` générique, `QrCodeIsActiveSpecification`, `UserIsCircleMemberSpecification`…
- **Shared** : `TimeProvider`, `DomainEvent`, `DomainException`, `BusinessRuleViolationException`

### `uzima-application`

**Rôle** : Orchestration des use cases. Pas de logique métier.

Contenu :
- **52 Use Cases** — un par action métier
- **Commands** (records) : `SendPaymentCommand`, `CreateReminderCommand`…
- **Ports in** : interfaces exposées aux contrôleurs
- **Ports out** : interfaces des dépendances techniques (repos, services IA)
- **Services applicatifs** : `BruteForceProtectionService`, `NotificationRouter`
- **Stratégies** : `NotificationRoutingStrategy` (NORMAL / DEFERRED / URGENT_ONLY / BLOCKED)

### `uzima-security`

**Rôle** : Module autonome de sécurité JWT avec Token Family Pattern.

Contenu :
- `JwtTokenService` : génération et vérification des access tokens
- `AccessTokenVerifierPort` : port consommé par le filtre HTTP
- `RefreshTokenHasherPort` : abstraction du hachage (SHA-256)
- Token Family : chaque famille de refresh tokens est invalidée à la moindre réutilisation suspecte

### `uzima-infrastructure`

**Rôle** : Implémentations techniques des ports.

Contenu :
- **Persistence** : JPA Entities, Entity Mappers, Spring Data Repositories, Repository Adapters
- **AI Stubs** : `StubTranscriptionAdapter`, `StubTranslationAdapter`, `StubIntentDetectionAdapter`, `StubEmotionAnalysisAdapter`, `StubConversationSummaryAdapter`, `StubGeolocationAdapter`, `StubCalendarAdapter`
- **Notification** : `ImmediateNotificationStrategy`, `DeferredNotificationStrategy`, `UrgentOnlyNotificationStrategy`, `BlockedNotificationStrategy`, `PresenceAwareNotificationAdapter`
- **Security** : `BCryptPasswordHasher`, `LibPhoneNumberAdapter`, `InMemoryLoginAttemptRepository`, `SecureRefreshTokenHasher`
- **Payment** : `FakePaymentGatewayAdapter` (MVP — à remplacer)

### `uzima-bootstrap`

**Rôle** : Point d'entrée Spring Boot. Câblage IoC, adaptateurs HTTP.

Contenu :
- **REST Controllers** (10) : `AuthController`, `TokenController`, `MessageController`, `QrCodeController`, `PaymentController`, `InvoiceController`, `ProjectController`, `CircleController`, `AssistantController`, `WellbeingController`
- **Configuration** : `DomainConfiguration` (beans use cases), `InfrastructureConfiguration` (beans adapters), `SecurityConfig` (Spring Security), `OpenApiConfiguration` (Swagger)
- **Exception Handler** : `GlobalExceptionHandler` (@RestControllerAdvice) — mapping domain exceptions → HTTP status codes
- **Filters** : `RateLimitingFilter` (rate limiting par IP), `JwtAuthenticationFilter`
- **Flyway** : migrations V1–V8

---

## DDD — Domain-Driven Design

### Ubiquitous Language par domaine

| Domaine | Concepts clés |
|---------|--------------|
| **qrcode** | QrCode, GeofenceRule, PersonalizationRule, ExpirationPolicy, ScanLimit |
| **message** | Message, Conversation, MessageMetadata, MessageType (TEXT/VOICE/IMAGE…) |
| **payment** | Transaction, Money, PaymentMethod, Currency |
| **social** | Circle, CircleType (FAMILY/WORK/FRIENDS/PROJECTS), CircleMembership, MemberRole |
| **workspace** | Project, Task, TimeEntry, TaskStatus (BACKLOG→IN_PROGRESS→IN_REVIEW→DONE) |
| **invoice** | Invoice, InvoiceItem, TaxRate, InvoiceStatus (DRAFT→SENT→PAID) |
| **assistant** | Reminder, ReminderTrigger, ReminderStatus (PENDING→TRIGGERED→SNOOZED→DISMISSED) |
| **wellbeing** | FocusSession, UsageSession, AppType, DigitalHealthMetrics, WellbeingReport |

### Domain Events

Les agrégats émettent des événements collectés via `pullDomainEvents()` :

```java
// Exemple : FocusSession
public void end(TimeProvider clock) {
    if (status.isTerminal()) throw new AlreadyEndedException(...);
    this.endedAt = clock.now();
    this.status = FocusSessionStatus.COMPLETED;
    domainEvents.add(new FocusSessionEndedEvent(
        EventId.generate(), this.id, this.userId,
        this.startedAt, this.endedAt, clock.now()
    ));
}

// Dans le Use Case
FocusSession session = repo.findById(cmd.sessionId());
session.end(clock);
repo.save(session);
// session.pullDomainEvents() → publier les événements si nécessaire
```

### Hiérarchie des exceptions

```
DomainException (abstract)
└── BusinessRuleViolationException

ApplicationException
├── ResourceNotFoundException  → 404
├── UnauthorizedException      → 403
└── ConflictException          → 409

InfrastructureException
├── DatabaseException
└── ExternalServiceException

(Exceptions domain spécifiques, ex:)
QrCode.QrCodeRevokedException  → 422
QrCode.OutsideGeofenceException → 403
Reminder.IllegalReminderTransitionException → 422
```

---

## Hexagonal — Ports & Adapters

### Exemple complet : ScanQrCodeUseCase

```
HTTP Request
    │
    ▼
QrCodeController (bootstrap/adapter/http)
    │  appelle
    ▼
ScanQrCodeUseCase (application)
    │  lit via
    ├─► QrCodeRepositoryPort (port out)
    │       └── QrCodeRepositoryAdapter (infrastructure) ──► PostgreSQL
    │  lit via
    ├─► GeolocationPort (port out)
    │       └── StubGeolocationAdapter (infrastructure) ──► (futur: GPS API)
    │  appelle
    └─► qrCode.recordScan() (domain)
```

### Ports out — interfaces application

```
application/qrcode/port/out/
├── QrCodeRepositoryPort       (save, findById, findByOwnerId)
├── GeolocationPort            (getCurrentLocation)
└── CalendarIntegrationPort    (getActiveEvent)

application/message/port/out/
├── MessageRepositoryPort      (save, findById, findByConversationId, findByDetectedIntent)
├── VoiceTranscriptionPort     (transcribe)
├── TranslationPort            (translate)
├── IntentDetectionPort        (detect)
├── EmotionAnalysisPort        (analyze)
├── ConversationSummaryPort    (summarize)
└── MessageNotificationPort    (notify)
```

---

## Flux d'une requête HTTP

### POST /api/qrcodes/{id}/scan

```
1. RateLimitingFilter         — vérification IP (20 req/min)
2. JwtAuthenticationFilter    — vérification Bearer token
3. QrCodeController           — désérialisation JSON, extraction path/query params
4. ScanQrCodeUseCase.execute()
   a. QrCodeRepositoryPort.findById()  → ResourceNotFoundException si absent
   b. Si qrCode.hasGeofence()
      - GeolocationPort.getCurrentLocation() → GeolocationUnavailableException si absent
      - geofenceRule.contains(lat, lon)      → OutsideGeofenceException si hors zone
   c. qrCode.recordScan(clock.now())         → QrCodeRevokedException / ExpiredException / ScanLimitReachedException
   d. QrCodeRepositoryPort.save(qrCode)
   e. return ScanResult(qrCodeId, type, ownerId, totalScans)
5. QrCodeController           — sérialisation Map<String, Object> → JSON 200
6. GlobalExceptionHandler     — si exception domain → HTTP status approprié
```

---

## Sécurité & Auth

### Token Family Pattern

```
Login
  │
  ├── Génère access_token (15 min, HS256)
  └── Génère refresh_token (opaque, hors JWT)
         │
         └── Stocké haché (SHA-256) en base + family_id

Refresh
  │
  ├── Vérifie hash du refresh_token en base
  ├── Invalide l'ancien token
  └── Génère nouveau access_token + nouveau refresh_token (même family_id)
         │
         └── Si réutilisation d'un token révoqué → invalide TOUTE la famille
                 (protection contre le vol de token)
```

### AccountLockoutPolicy (domain)

```java
// Verrouillage progressif par identifiant (numéro de téléphone)
public LockoutDecision evaluate(LoginAttemptHistory history) {
    int failures = history.recentFailures();
    if (failures >= 8) return LockoutDecision.hardLock(Duration.ofHours(24));
    if (failures >= 5) return LockoutDecision.mediumLock(Duration.ofMinutes(30));
    if (failures >= 3) return LockoutDecision.softLock(Duration.ofMinutes(5));
    return LockoutDecision.allow();
}
```

---

## Gestion des notifications

Pattern Strategy câblé dans `InfrastructureConfiguration` :

```
PresenceAwareNotificationAdapter
    │
    └── NotificationRouter
            │
            ├── ImmediateNotificationStrategy  ← AVAILABLE, TIRED, TRAVELING
            ├── DeferredNotificationStrategy   ← FOCUSED (mise en queue)
            ├── UrgentOnlyNotificationStrategy ← SILENCE (urgences seulement)
            └── BlockedNotificationStrategy    ← OFFLINE (rien)
```

Le routage est déterminé par le `PresenceStatus` du destinataire, lu via `UserRepositoryPort`.

---

## Stratégie IA (MVP)

Les ports IA sont tous implémentés par des **stubs déterministes** en développement. Chaque stub peut être remplacé indépendamment par un adaptateur réel (OpenAI, Whisper, etc.) sans modifier le domain ou l'application.

| Port | Stub actuel | Remplacement prod prévu |
|------|-------------|------------------------|
| `VoiceTranscriptionPort` | `StubTranscriptionAdapter` | OpenAI Whisper API |
| `TranslationPort` | `StubTranslationAdapter` | DeepL / Google Translate |
| `IntentDetectionPort` | `StubIntentDetectionAdapter` (heuristiques mots-clés) | GPT-4 / Claude |
| `EmotionAnalysisPort` | `StubEmotionAnalysisAdapter` | Hume AI |
| `ConversationSummaryPort` | `StubConversationSummaryAdapter` | GPT-4 / Claude |
| `GeolocationPort` | `StubGeolocationAdapter` (empty) | GPS mobile push |
| `CalendarIntegrationPort` | `StubCalendarAdapter` (empty) | Google Calendar API |

---

## Base de données

### Schéma des migrations Flyway

```
V1 : users
     ├── id (UUID PK)
     ├── phone_number (UNIQUE, libphonenumber validé)
     ├── name, password_hash
     ├── presence_status (CHECK enum)
     └── login_attempts, locked_until

V2 : transactions
     ├── sender_id, recipient_id (FK → users)
     ├── amount (NUMERIC ≥ 0), currency (CHECK enum)
     ├── status, payment_method (CHECK enums)
     └── CHECK (sender_id ≠ recipient_id)

V3 : circles + circle_memberships
     ├── circles(type, visibility, notification_policy)
     └── circle_memberships(circle_id, user_id, role) UNIQUE

V4 : projects + tasks + time_entries
     ├── tasks(status CHECK, priority CHECK, project_id FK)
     └── time_entries(started_at, ended_at, CHECK ended > started)

V5 : invoices + invoice_items
     ├── invoices(status CHECK, issuer_id FK → users)
     └── invoice_items(tax_rate CHECK enum, unit_price ≥ 0)

V6 : reminders + focus_sessions + usage_sessions
     ├── reminders(status CHECK, trigger CHECK)
     ├── focus_sessions(status CHECK, interruption_reason)
     └── usage_sessions(app_type CHECK enum)

V7 : ALTER TABLE messages
     └── ADD COLUMNS metadata_transcription, metadata_translation,
                       metadata_target_language, metadata_intent, metadata_emotion
         + INDEX partiel sur metadata_intent WHERE NOT NULL

V8 : qr_codes
     ├── type CHECK (PROFESSIONAL|SOCIAL|PAYMENT|TEMPORARY_LOCATION|EVENT|MEDICAL_EMERGENCY)
     ├── Géofencing : geofence_latitude, geofence_longitude, geofence_radius_meters
     │   CONSTRAINT geofence_consistency : tout null OU tout non-null
     └── Personnalisation : personalization_condition, personalization_target_profile
         CONSTRAINT personalization_consistency : les deux null OU les deux non-null
```

---

## Conventions et règles

### Règle 1 — Interdictions strictes

```java
// ❌ INTERDIT dans domain/ et application/
Instant.now()
LocalDate.now()
@Builder
@Setter
@Data

// ✅ AUTORISÉ dans infrastructure/ uniquement
@Getter
@NoArgsConstructor
```

### Règle 2 — Structure des Use Cases

```java
public class ScanQrCodeUseCase {

    private final QrCodeRepositoryPort qrCodeRepository;
    private final GeolocationPort geolocationPort;
    private final TimeProvider clock;

    public ScanResult execute(ScanQrCodeCommand cmd) {
        // 1. Charger l'agrégat
        QrCode qrCode = qrCodeRepository.findById(cmd.qrCodeId())
            .orElseThrow(() -> new ResourceNotFoundException("QrCode", cmd.qrCodeId()));

        // 2. Vérification applicative (autorisation, contexte)
        if (qrCode.hasGeofence()) { ... }

        // 3. Déléguer au domaine
        qrCode.recordScan(clock.now());

        // 4. Persister
        qrCodeRepository.save(qrCode);

        // 5. Retourner une projection (pas l'agrégat)
        return new ScanResult(...);
    }
}
```

### Règle 3 — Mappers

Chaque couche a son propre mapper :
- `UserEntityMapper` (infrastructure) : `User ↔ UserJpaEntity`
- `UserHttpMapper` (bootstrap) : `RegisterRequest → Command`, `User → UserResponse`

Les agrégats ne sont **jamais** sérialisés directement en JSON.

---

## Décisions d'architecture

| Date | Décision | Raison |
|------|----------|--------|
| 2026-03-12 | Maven multi-modules (5 modules) | Séparation stricte des dépendances, DIP enforced par le compilateur |
| 2026-03-12 | `TimeProvider` injectable | Testabilité — les tests contrôlent le temps |
| 2026-03-12 | `record` Java pour Value Objects | Immutabilité, `equals()`/`hashCode()` gratuits |
| 2026-03-12 | Interdiction `@Builder`/`@Setter` | Invariants protégés — construction via factory methods uniquement |
| 2026-03-12 | `DomainEvent` = interface | Les records Java ne peuvent pas `extends` une classe abstraite |
| 2026-03-12 | Token Family Pattern | Invalidation de session en cas de vol de refresh token |
| 2026-03-13 | `MessageMetadata.mergeWith()` — "this wins" | Les premières enrichissements IA ne sont pas écrasés par les suivants |
| 2026-03-13 | Stubs AI déterministes | Développement local sans clés API, remplacement indépendant en prod |
| 2026-03-13 | `GeolocationPort` retourne `Optional.empty()` en stub | Force l'utilisation d'un vrai adaptateur en prod pour les QR géofencés |
| 2026-03-13 | Springdoc 2.6.0 pour OpenAPI | Documentation interactive auto-générée depuis les annotations REST |
