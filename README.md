# Uzima — Super-App Intelligente

Uzima est une super-application sociale africaine combinant messagerie enrichie par IA, QR codes contextuels intelligents, paiements intégrés, gestion de projets freelance et outils de bien-être numérique.

---

## Sommaire

- [Architecture](#architecture)
- [Démarrage rapide](#démarrage-rapide)
- [Structure du projet](#structure-du-projet)
- [Stack technique](#stack-technique)
- [Domaines métier](#domaines-métier)
- [API REST](#api-rest)
- [Sécurité](#sécurité)
- [Base de données](#base-de-données)
- [Tests](#tests)
- [Conventions](#conventions)
- [Documentation](#documentation)

---

## Architecture

Uzima backend suit une architecture **DDD + Clean Architecture + Hexagonal (Ports & Adapters)** organisée en modules Maven strictement isolés.

```
┌─────────────────────────────────────────────────────────┐
│                      bootstrap                          │  ← Spring Boot, REST, Config
├─────────────────────────────────────────────────────────┤
│                    infrastructure                       │  ← JPA, JWT, Stubs AI
├──────────────────────────┬──────────────────────────────┤
│       application        │         security             │  ← Use Cases, Ports
├──────────────────────────┴──────────────────────────────┤
│                        domain                           │  ← 0 dépendances externes
└─────────────────────────────────────────────────────────┘
         Dépendances : vers le bas uniquement
```

**Règle absolue** : le module `domain` ne dépend d'aucune bibliothèque externe (ni Spring, ni JPA, ni Lombok). Il compile avec le JDK seul.

Voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) pour la documentation complète.

---

## Démarrage rapide

### Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java | 25 |
| Maven | 3.9+ |
| PostgreSQL | 15+ |
| Docker (optionnel) | 24+ |

### 1. Démarrer l'infrastructure

```bash
# PostgreSQL + Redis uniquement (mode dev)
docker-compose up -d postgres redis

# Ou stack complète avec le backend (nécessite un Dockerfile dans backend/)
docker-compose --profile full up -d
```

### 2. Configurer les variables d'environnement

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=uzima-secret-key-CHANGE-IN-PRODUCTION-min-32-chars
```

### 3. Lancer le backend

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run -pl bootstrap
```

API disponible : `http://localhost:8080`
Swagger UI : `http://localhost:8080/swagger-ui.html`
OpenAPI JSON : `http://localhost:8080/v3/api-docs`

### 4. Lancer le frontend mobile

```bash
cd mobile
npm install
npm run android   # ou: npm run ios
```

---

## Structure du projet

```
uzima/
├── backend/                        # Backend Java — Maven multi-modules
│   ├── domain/                     # Logique métier pure (0 dépendances)
│   │   └── src/
│   │       ├── main/java/com/uzima/domain/
│   │       │   ├── user/           # Agrégat User, PresenceStatus
│   │       │   ├── message/        # Agrégat Message, MessageMetadata
│   │       │   ├── qrcode/         # Agrégat QrCode, GeofenceRule, PersonalizationRule
│   │       │   ├── payment/        # Agrégat Transaction, Money (VO)
│   │       │   ├── social/         # Agrégat Circle, CircleMembership
│   │       │   ├── workspace/      # Agrégats Project, Task, TimeEntry
│   │       │   ├── invoice/        # Agrégats Invoice, InvoiceItem
│   │       │   ├── assistant/      # Agrégat Reminder
│   │       │   ├── wellbeing/      # Agrégats FocusSession, UsageSession, DigitalHealthMetrics
│   │       │   ├── security/       # AccountLockoutPolicy
│   │       │   └── shared/         # TimeProvider, DomainEvent, DomainException, Specification<T>
│   │       └── test/               # Tests domain (JUnit5 + AssertJ, sans Spring)
│   │
│   ├── application/                # Use Cases et orchestration
│   │   └── src/main/java/com/uzima/application/
│   │       ├── user/               # RegisterUserUseCase, UpdatePresenceUseCase…
│   │       ├── message/            # SendMessage, Transcribe, Translate, DetectIntent…
│   │       ├── qrcode/             # CreateQrCode, ScanQrCode, SuggestQrCodeType…
│   │       ├── payment/            # SendPayment, RequestPayment, CancelTransaction…
│   │       ├── social/             # CreateCircle, AddMember, SuggestCircle…
│   │       ├── workspace/          # CreateProject, CreateTask, TrackTime…
│   │       ├── invoice/            # CreateInvoice, SendInvoice, MarkPaid…
│   │       ├── assistant/          # CreateReminder, TriggerReminder, SnoozeReminder…
│   │       ├── wellbeing/          # StartFocusSession, TrackAppUsage, GetWellbeingReport…
│   │       ├── notification/       # NotificationRouter, NotificationRoutingStrategy
│   │       └── security/           # BruteForceProtectionService
│   │
│   ├── security/                   # Module JWT (Token Family + Refresh Token Rotation)
│   │   └── src/main/java/com/uzima/security/
│   │       └── token/              # JwtTokenService, AccessTokenVerifierPort, RefreshTokenHasherPort
│   │
│   ├── infrastructure/             # Adaptateurs techniques
│   │   └── src/main/java/com/uzima/infrastructure/
│   │       ├── persistence/        # JPA Entities, Mappers, Spring Data Repositories, Adapters
│   │       ├── ai/                 # Stubs IA (Transcription, Translation, Intent, Emotion…)
│   │       ├── notification/       # Strategies (Immediate, Deferred, UrgentOnly, Blocked)
│   │       ├── payment/            # FakePaymentGatewayAdapter
│   │       └── security/           # BCrypt, LibPhoneNumber, InMemoryLoginAttemptRepository
│   │
│   └── bootstrap/                  # Point d'entrée Spring Boot
│       └── src/
│           ├── main/java/com/uzima/bootstrap/
│           │   ├── adapter/http/   # REST Controllers (10 controllers)
│           │   └── config/         # DomainConfiguration, InfrastructureConfiguration, SecurityConfig, OpenApiConfiguration
│           └── main/resources/
│               ├── application.yml
│               └── db/migration/   # Flyway V1–V8
│
├── mobile/                         # Application React Native
├── docs/                           # Documentation technique
│   ├── ARCHITECTURE.md             # Architecture DDD/Clean/Hexagonal détaillée
│   ├── API.md                      # Référence complète des endpoints REST
│   └── SETUP.md                    # Guide d'installation et configuration
└── docker-compose.yml              # PostgreSQL + Redis + Backend (profil full)
```

---

## Stack technique

### Backend

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 25 |
| Framework | Spring Boot | 3.5.0 |
| Base de données | PostgreSQL | 15+ |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Migrations DB | Flyway | 10.x |
| Auth | JWT (JJWT) | 0.12.6 |
| Validation téléphone | libphonenumber (Google) | 8.13.50 |
| Cache / Sessions | Redis | 7+ |
| Real-time | WebSocket (Netty-SocketIO) | 2.0.9 |
| Documentation API | Springdoc OpenAPI | 2.6.0 |
| Build | Maven | 3.9+ |
| Tests | JUnit 5 + AssertJ + Mockito | 5.11 / 3.26 / 5.12 |

### Mobile

| Composant | Technologie |
|-----------|-------------|
| Framework | React Native 0.73 |
| Langage | TypeScript |
| State Management | Zustand + React Query |
| Base locale | WatermelonDB |
| Real-time | Socket.IO Client |

---

## Domaines métier

### Couverture des features : 45/55 (82%)

| Domaine | Features | Statut |
|---------|----------|--------|
| QR Codes Contextuels | F1.1–F1.6 | 6/6 ✅ |
| Messagerie IA | F2.1–F2.11 | 8/11 (3 partiel/TODO) |
| Paiements | F3.1–F3.11 | 1/11 (10 TODO) |
| Social (Cercles de Vie) | F4.1–F4.6 | 2/6 (4 TODO) |
| Assistant IA | F5.1–F5.6 | 1/6 + 1 partiel (4 TODO) |
| Workspace Freelance | F6.1–F6.6 | 2/6 (4 TODO) |
| Bien-être numérique | F7.1–F7.6 | 4/6 + 1 partiel (2 TODO) |

### Agrégats principaux

| Agrégat | Domaine | Comportements clés |
|---------|---------|-------------------|
| `User` | user | `register()`, `updatePresence()`, `updateProfile()` |
| `Message` | message | `send()`, `enrich(MessageMetadata)`, `delete()` |
| `QrCode` | qrcode | `create()`, `recordScan()`, `revoke()`, `configureRules()` |
| `Transaction` | payment | `initiate()`, `complete()`, `fail()`, `cancel()` |
| `Circle` | social | `create()`, `addMember()`, `removeMember()`, `updateRules()` |
| `Project` | workspace | `create()`, `addMember()`, `addTask()`, `trackTime()` |
| `Task` | workspace | `start()`, `submit()`, `approve()`, `block()`, `reopen()` |
| `Invoice` | invoice | `draft()`, `send()`, `markPaid()`, `cancel()` |
| `Reminder` | assistant | `create()`, `trigger()`, `snooze()`, `dismiss()` |
| `FocusSession` | wellbeing | `start()`, `end()`, `interrupt()` |

---

## API REST

Base URL : `http://localhost:8080/api`
Authentification : `Authorization: Bearer <access_token>`

### Endpoints par domaine

| Domaine | Préfixe | Méthodes principales |
|---------|---------|---------------------|
| Auth | `/api/auth` | POST register, login |
| Tokens | `/api/auth/token` | POST refresh, DELETE revoke |
| Messages | `/api/messages` | POST send, GET list, POST transcribe/translate/detect-intent… |
| QR Codes | `/api/qrcodes` | POST create, GET list, POST scan/revoke, PUT rules, GET suggest |
| Paiements | `/api/payments` | POST send/request, GET history, DELETE cancel |
| Factures | `/api/invoices` | POST create, GET list/detail, POST send/paid/cancel |
| Projets | `/api/projects` | POST create, GET kanban/report, POST tasks, PUT task-status |
| Cercles | `/api/circles` | POST create, GET list, POST add-member, DELETE remove-member |
| Assistant | `/api/assistant/reminders` | POST create, GET list/active, POST snooze/dismiss |
| Bien-être | `/api/wellbeing` | GET report, POST focus/start/end/interrupt, POST usage |

Référence complète : [docs/API.md](docs/API.md)
Interface interactive : `http://localhost:8080/swagger-ui.html`

---

## Sécurité

### Authentification JWT (Stateless)

- **Access Token** : durée de vie 15 min (configurable via `JWT_EXPIRATION_SECONDS`)
- **Refresh Token** : rotation à chaque renouvellement (Token Family pattern)
- **Stockage** : tokens hachés en base (SHA-256 via `SecureRefreshTokenHasher`)

### Protection brute-force (double couche)

| Couche | Mécanisme | Portée |
|--------|-----------|--------|
| HTTP | `RateLimitingFilter` | 20 req/min par IP sur `/api/auth/**` |
| Domaine | `AccountLockoutPolicy` | Verrouillage progressif par identifiant (3/5/8 échecs) |

Niveaux de verrouillage : **SOFT** (3 échecs, 5 min) → **MEDIUM** (5 échecs, 30 min) → **HARD** (8 échecs, 24h).

### Headers de sécurité OWASP

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
Referrer-Policy: strict-origin-when-cross-origin
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

### CORS

Origines autorisées : `http://localhost:*` (dev) + `https://*.uzima.app` (prod).

---

## Base de données

### Migrations Flyway

| Version | Fichier | Contenu |
|---------|---------|---------|
| V1 | `V1__create_users_table.sql` | Table `users`, présence, sécurité |
| V2 | `V2__create_transactions_table.sql` | Table `transactions`, contraintes CHECK |
| V3 | `V3__create_circles_table.sql` | Tables `circles`, `circle_memberships` |
| V4 | `V4__create_workspace_tables.sql` | Tables `projects`, `tasks`, `time_entries` |
| V5 | `V5__create_invoices_table.sql` | Tables `invoices`, `invoice_items` |
| V6 | `V6__create_assistant_wellbeing_tables.sql` | Tables `reminders`, `focus_sessions`, `usage_sessions` |
| V7 | `V7__add_message_metadata.sql` | Colonnes IA sur `messages` (transcription, intent…) |
| V8 | `V8__create_qrcodes_table.sql` | Table `qr_codes`, géofencing, personnalisation |

### Configuration

```yaml
# application.yml — variables d'environnement obligatoires en production
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uzima
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

uzima:
  security:
    jwt:
      secret: ${JWT_SECRET}          # min 32 caractères
      expiration-seconds: ${JWT_EXPIRATION_SECONDS:900}
```

---

## Tests

### Structure des tests

```bash
# Tests domain (sans Spring, sans base de données)
mvn test -pl domain

# Tests application (Mockito)
mvn test -pl application

# Tous les tests
mvn test
```

### Couverture actuelle

| Module | Couverture | Cible |
|--------|-----------|-------|
| domain | ~93% | 95%+ |
| application | ~87% | 90%+ |

### Fichiers de tests domain

| Fichier | Tests | Domaine |
|---------|-------|---------|
| `UserTest.java` | 20+ | user |
| `ConversationTest.java` | 15+ | message |
| `MessageEnrichmentTest.java` | 22 | message/IA |
| `QrCodeTest.java` | 35+ | qrcode |
| `QrCodeFactoryTest.java` | 10+ | qrcode |
| `MoneyTest.java` | 8 | payment |
| `TransactionTest.java` | 10 | payment |
| `CircleTest.java` | 26 | social |
| `TaskTest.java` | 14 | workspace |
| `ProjectTest.java` | 16 | workspace |
| `InvoiceTest.java` | 17 | invoice |
| `ReminderTest.java` | 20 | assistant |
| `FocusSessionTest.java` | 16 | wellbeing |
| `AccountLockoutPolicyTest.java` | 12+ | security |

---

## Conventions

### Règles strictes

| Règle | Portée | Justification |
|-------|--------|---------------|
| `0 Instant.now()` | `domain/`, `application/` | Testabilité déterministe via `TimeProvider` |
| `0 @Builder` | partout | Protège les invariants, force les factory methods |
| `0 @Setter` | partout | Immutabilité des Value Objects, constructeurs privés sur Aggregates |
| `@Getter` Lombok | `infrastructure/` uniquement | Réduction boilerplate sur JPA Entities |
| Value Objects = `record` | `domain/` | Immutabilité garantie par le compilateur |
| Constructeurs `private` | Aggregates | Toute construction passe par `create()` ou `reconstitute()` |

### Patterns utilisés

- **Factory** : `User.register()`, `QrCodeFactory.createProfessional()`, `Transaction.initiate()`
- **Specification** : `QrCodeIsActiveSpecification`, `UserIsCircleMemberSpecification`
- **Strategy** : `NotificationRoutingStrategy` (NORMAL / DEFERRED / URGENT_ONLY / BLOCKED)
- **Adapter** : tous les `*RepositoryAdapter`, `*EntityMapper`
- **Domain Events** : `TransactionCompletedEvent`, `ReminderTriggeredEvent`, `FocusSessionEndedEvent`
- **Port/Adapter (Hexagonal)** : ports `in/` (use cases) et `out/` (repositories, services externes)

### Système de notifications (Sprint 7)

Le routage des notifications suit une stratégie basée sur le statut de présence de l'utilisateur :

| Stratégie | Déclencheur | Comportement |
|-----------|-------------|--------------|
| `IMMEDIATE` | Utilisateur en ligne | Envoi WebSocket direct via `SocketIONotificationAdapter` |
| `DEFERRED` | Utilisateur absent | Mise en file `DeferredQueueFlusherService`, flush toutes les 60s |
| `URGENT_ONLY` | Mode focus | Seules les notifications urgentes sont transmises |
| `BLOCKED` | DND actif | Aucune notification |

- **Profil `dev`** : `LoggingNotificationAdapter` (noop, pas de socket nécessaire)
- **Profil `prod`** : `SocketIONotificationAdapter` via Netty-SocketIO (port `9092`)

---

## Documentation

| Document | Contenu |
|----------|---------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture DDD/Clean/Hexagonal, décisions techniques, flux de données |
| [docs/API.md](docs/API.md) | Référence complète des endpoints REST avec exemples |
| [docs/SETUP.md](docs/SETUP.md) | Installation, configuration, déploiement |
| [backend/TODO.md](backend/TODO.md) | Progression par sprint, features implémentées, roadmap |
| `http://localhost:8080/swagger-ui.html` | Documentation interactive (runtime) |

---

## Progression

**45/55 features implémentées (82%)**

```
Sprint 1-2   ✅  Payment domain
Sprint 3-4   ✅  Social domain (Cercles de Vie)
Sprint 5-6   ✅  Workspace + Invoice
Sprint 7-8   ✅  Assistant IA + Bien-être numérique
Sprint 9-10  ✅  Messagerie IA (Transcription, Traduction, Intention, Émotion, Résumé)
Sprint 11-12 ✅  QR Code Avancé + Polish (OpenAPI, CORS, Security Headers)
Sprint 7*    ✅  Refactoring notifications (WebSocket, Strategies, DeferredQueue, Scheduler)
```

> \* Sprint technique transverse — refactoring du système de notifications en architecture Ports & Adapters.
