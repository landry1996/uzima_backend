# 🏗️ PLAN DE REFONTE BACKEND UZIMA
## Conformité DDD / Clean Architecture / Hexagonal

**Objectif**: Compléter les 71% de fonctionnalités manquantes tout en maintenant l'excellence architecturale actuelle

---

## ✅ ÉTAT ACTUEL - CE QUI EST EXCELLENT

### Architecture Respectée à 100%

Le backend actuel est **un modèle de Clean Architecture** :

```
✅ Domain Module (0 dépendance externe)
├── Pas de Spring
├── Pas de JPA/Hibernate  
├── Pas de Lombok
├── TimeProvider injectable partout
└── Tests unitaires sans Spring

✅ Separation of Concerns
├── domain/ = Logique métier pure
├── application/ = Use Cases orchestration
├── infrastructure/ = Adapters techniques
└── bootstrap/ = Configuration Spring

✅ DDD Strict
├── Aggregates (User, Message, QrCode)
├── Value Objects (PhoneNumber, Money, etc.)
├── Factories métier (UserFactory, QrCodeFactory)
├── Specifications (QrCodeExpiredSpec, etc.)
└── Domain Events (prévu)

✅ Hexagonal
├── Ports IN (Use Case interfaces)
├── Ports OUT (Repository interfaces)
└── Adapters (JPA, HTTP, Notification)

✅ Qualité Code
├── Immutabilité (final fields partout)
├── Invariants protégés (factory methods)
├── Pas de @Builder, pas de @Setter
└── Constructeurs privés
```

---

## 🎯 OBJECTIFS REFONTE

### Conserver l'Excellence + Ajouter Features

**NE PAS TOUCHER** :
- ✅ Structure modules (domain/application/infrastructure)
- ✅ Patterns DDD actuels
- ✅ Qualité code
- ✅ Tests existants

**AJOUTER** :
- 📦 5 nouveaux domaines (payment, social, workspace, assistant, wellbeing)
- 🔧 45 nouveaux Use Cases
- 🧪 Tests pour toutes les nouvelles features
- 🔌 Adapters infrastructure (Stripe, OpenAI, etc.)

---

## 📋 ROADMAP DÉTAILLÉE (12 Sprints)

### 🚀 Sprint 1-2 : Payment Domain (CRITIQUE)

**Objectif**: Paiements de base fonctionnels

#### Livrables Sprint 1 :

```
domain/payment/
├── model/
│   ├── Transaction.java              # Aggregate Root
│   ├── TransactionId.java            # VO
│   ├── Money.java                    # VO  
│   ├── Currency.java                 # Enum
│   ├── TransactionStatus.java        # Enum (PENDING, COMPLETED, FAILED)
│   └── PaymentMethod.java            # Enum (MOBILE_MONEY, CARD, CRYPTO)
├── port/
│   ├── TransactionRepository.java    # OUT
│   └── PaymentGatewayPort.java       # OUT
├── factory/
│   └── TransactionFactory.java
└── specification/
    ├── TransactionCompletedSpec.java
    └── SufficientFundsSpec.java

application/payment/
├── SendPaymentUseCase.java
├── RequestPaymentUseCase.java
├── GetTransactionHistoryUseCase.java
├── port/in/
│   ├── SendPaymentCommand.java
│   └── RequestPaymentCommand.java
└── port/out/
    └── TransactionRepositoryPort.java

infrastructure/payment/
├── persistence/
│   ├── TransactionJpaEntity.java
│   ├── TransactionEntityMapper.java
│   ├── TransactionRepositoryAdapter.java
│   └── SpringDataTransactionRepository.java
└── gateway/
    ├── StripePaymentAdapter.java      # (fake pour MVP)
    └── MobileMoneyAdapter.java        # (fake pour MVP)

bootstrap/adapter/http/
└── PaymentController.java
    ├── POST /api/payments/send
    ├── POST /api/payments/request
    └── GET /api/payments/history
```

#### Code Template Transaction :

```java
package com.uzima.domain.payment.model;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root : Transaction de paiement.
 * 
 * Invariants :
 * - Montant toujours positif (validé par Money VO)
 * - Sender != Recipient
 * - Une transaction COMPLETED ne peut plus changer
 * - Le temps est injectable (pas de Instant.now())
 */
public final class Transaction {

    private final TransactionId id;
    private final UserId senderId;
    private final UserId recipientId;
    private final Money amount;
    private final PaymentMethod method;
    private final Instant initiatedAt;
    
    // État mutable
    private TransactionStatus status;
    private Instant completedAt;
    private String externalTransactionId; // ID du provider (Stripe, MPesa)

    private Transaction(
            TransactionId id,
            UserId senderId,
            UserId recipientId,
            Money amount,
            PaymentMethod method,
            Instant initiatedAt
    ) {
        // Validation invariants
        Objects.requireNonNull(id);
        Objects.requireNonNull(senderId);
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(method);
        Objects.requireNonNull(initiatedAt);
        
        if (senderId.equals(recipientId)) {
            throw new SelfPaymentNotAllowedException(
                "Impossible d'envoyer de l'argent à soi-même"
            );
        }
        
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
        this.method = method;
        this.initiatedAt = initiatedAt;
        this.status = TransactionStatus.PENDING;
    }

    // Factory method : Initier un paiement
    public static Transaction initiate(
            UserId sender,
            UserId recipient,
            Money amount,
            PaymentMethod method,
            TimeProvider clock
    ) {
        Objects.requireNonNull(clock, "TimeProvider obligatoire");
        return new Transaction(
            TransactionId.generate(),
            sender,
            recipient,
            amount,
            method,
            clock.now()
        );
    }

    // Reconstitution depuis DB
    public static Transaction reconstitute(
            TransactionId id,
            UserId senderId,
            UserId recipientId,
            Money amount,
            PaymentMethod method,
            TransactionStatus status,
            Instant initiatedAt,
            Instant completedAt,
            String externalTransactionId
    ) {
        Transaction tx = new Transaction(id, senderId, recipientId, amount, method, initiatedAt);
        tx.status = status;
        tx.completedAt = completedAt;
        tx.externalTransactionId = externalTransactionId;
        return tx;
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Marque la transaction comme complétée.
     * Règle métier : une transaction ne peut être complétée qu'une seule fois.
     */
    public void complete(String externalTxId, TimeProvider clock) {
        Objects.requireNonNull(externalTxId, "L'ID de transaction externe est obligatoire");
        Objects.requireNonNull(clock, "TimeProvider obligatoire");
        
        if (this.status == TransactionStatus.COMPLETED) {
            throw new TransactionAlreadyCompletedException(
                "Transaction " + id + " déjà complétée"
            );
        }
        
        if (this.status == TransactionStatus.FAILED) {
            throw new CannotCompleteFailedTransactionException(
                "Impossible de compléter une transaction échouée"
            );
        }
        
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = clock.now();
        this.externalTransactionId = externalTxId;
        
        // TODO: Lever DomainEvent TransactionCompleted
    }

    /**
     * Marque la transaction comme échouée.
     */
    public void fail(TimeProvider clock) {
        Objects.requireNonNull(clock);
        
        if (this.status != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                "Seules les transactions PENDING peuvent échouer"
            );
        }
        
        this.status = TransactionStatus.FAILED;
        this.completedAt = clock.now();
    }

    /**
     * Règle métier : Une transaction peut être annulée uniquement si PENDING.
     */
    public boolean canBeCancelled() {
        return this.status == TransactionStatus.PENDING;
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public TransactionId id() { return id; }
    public UserId senderId() { return senderId; }
    public UserId recipientId() { return recipientId; }
    public Money amount() { return amount; }
    public PaymentMethod method() { return method; }
    public TransactionStatus status() { return status; }
    public Instant initiatedAt() { return initiatedAt; }
    public java.util.Optional<Instant> completedAt() { 
        return java.util.Optional.ofNullable(completedAt); 
    }
    public java.util.Optional<String> externalTransactionId() {
        return java.util.Optional.ofNullable(externalTransactionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction t)) return false;
        return id.equals(t.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class SelfPaymentNotAllowedException extends DomainException {
        public SelfPaymentNotAllowedException(String msg) { super(msg); }
    }

    public static final class TransactionAlreadyCompletedException extends DomainException {
        public TransactionAlreadyCompletedException(String msg) { super(msg); }
    }

    public static final class CannotCompleteFailedTransactionException extends DomainException {
        public CannotCompleteFailedTransactionException(String msg) { super(msg); }
    }

    public static final class InvalidTransactionStateException extends DomainException {
        public InvalidTransactionStateException(String msg) { super(msg); }
    }
}
```

#### Value Object Money :

```java
package com.uzima.domain.payment.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object : Montant monétaire.
 * 
 * Invariants :
 * - Montant jamais négatif
 * - Précision 2 décimales (centimes)
 * - Currency obligatoire
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "Le montant ne peut pas être nul");
        Objects.requireNonNull(currency, "La devise ne peut pas être nulle");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountException(
                "Le montant ne peut pas être négatif : " + amount
            );
        }
        
        // Arrondir à 2 décimales
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money ofCents(long cents, Currency currency) {
        return new Money(
            BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100)),
            currency
        );
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // -------------------------------------------------------------------------
    // Opérations
    // -------------------------------------------------------------------------

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        ensureSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                "Résultat négatif : " + this + " - " + other
            );
        }
        return new Money(result, this.currency);
    }

    public Money multiply(double factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Le multiplicateur ne peut pas être négatif");
        }
        return new Money(
            this.amount.multiply(BigDecimal.valueOf(factor)),
            this.currency
        );
    }

    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void ensureSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                "Devises incompatibles : " + this.currency + " vs " + other.currency
            );
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class NegativeAmountException extends IllegalArgumentException {
        public NegativeAmountException(String msg) { super(msg); }
    }

    public static final class CurrencyMismatchException extends IllegalArgumentException {
        public CurrencyMismatchException(String msg) { super(msg); }
    }

    public static final class InsufficientFundsException extends IllegalArgumentException {
        public InsufficientFundsException(String msg) { super(msg); }
    }
}
```

#### Use Case SendPayment :

```java
package com.uzima.application.payment;

import com.uzima.application.payment.port.in.SendPaymentCommand;
import com.uzima.application.payment.port.out.*;
import com.uzima.domain.payment.model.*;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.domain.payment.port.TransactionRepository;
import com.uzima.domain.shared.TimeProvider;
import java.util.Objects;

/**
 * Use Case : Envoyer un paiement.
 * 
 * Orchestration :
 * 1. Créer Transaction (domain)
 * 2. Persister
 * 3. Appeler gateway paiement
 * 4. Mettre à jour statut
 * 5. Notifier (TODO)
 */
public class SendPaymentUseCase {

    private final TransactionRepository transactionRepository;
    private final PaymentGatewayPort paymentGateway;
    private final TimeProvider clock;

    public SendPaymentUseCase(
            TransactionRepository transactionRepository,
            PaymentGatewayPort paymentGateway,
            TimeProvider clock
    ) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
        this.clock = Objects.requireNonNull(clock);
    }

    public TransactionId execute(SendPaymentCommand command) {
        Objects.requireNonNull(command);

        // 1. Créer transaction via factory métier
        Transaction transaction = Transaction.initiate(
            command.senderId(),
            command.recipientId(),
            command.amount(),
            command.paymentMethod(),
            clock
        );

        // 2. Persister (état PENDING)
        transactionRepository.save(transaction);

        try {
            // 3. Appeler gateway externe (Stripe, MPesa, etc.)
            PaymentResult result = paymentGateway.processPayment(transaction);

            if (result.isSuccess()) {
                // 4. Marquer comme complété
                transaction.complete(result.externalTransactionId(), clock);
                transactionRepository.save(transaction);
                
                // TODO: Lever TransactionCompletedEvent
            } else {
                // 5. Marquer comme échoué
                transaction.fail(clock);
                transactionRepository.save(transaction);
                
                throw new PaymentFailedException(
                    "Paiement refusé : " + result.errorMessage()
                );
            }

        } catch (Exception e) {
            // En cas d'erreur technique
            transaction.fail(clock);
            transactionRepository.save(transaction);
            throw new PaymentGatewayException("Erreur gateway : " + e.getMessage(), e);
        }

        return transaction.id();
    }

    // Exceptions applicatives
    public static final class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String msg) { super(msg); }
    }

    public static final class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(String msg, Throwable cause) { super(msg, cause); }
    }
}
```

#### Tests Domain (sans Spring) :

```java
package com.uzima.domain.payment;

import com.uzima.domain.payment.model.*;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class TransactionTest {

    private final TimeProvider fixedClock = () -> Instant.parse("2026-03-12T10:00:00Z");

    @Test
    void should_initiate_transaction_with_valid_data() {
        // Given
        UserId sender = UserId.generate();
        UserId recipient = UserId.generate();
        Money amount = Money.of(100.50, Currency.EUR);

        // When
        Transaction tx = Transaction.initiate(
            sender, recipient, amount, PaymentMethod.CARD, fixedClock
        );

        // Then
        assertThat(tx.id()).isNotNull();
        assertThat(tx.senderId()).isEqualTo(sender);
        assertThat(tx.recipientId()).isEqualTo(recipient);
        assertThat(tx.amount()).isEqualTo(amount);
        assertThat(tx.status()).isEqualTo(TransactionStatus.PENDING);
        assertThat(tx.initiatedAt()).isEqualTo(fixedClock.now());
    }

    @Test
    void should_throw_exception_when_self_payment() {
        // Given
        UserId user = UserId.generate();
        Money amount = Money.of(50, Currency.EUR);

        // When / Then
        assertThatThrownBy(() -> 
            Transaction.initiate(user, user, amount, PaymentMethod.CARD, fixedClock)
        )
        .isInstanceOf(Transaction.SelfPaymentNotAllowedException.class)
        .hasMessageContaining("soi-même");
    }

    @Test
    void should_complete_transaction_successfully() {
        // Given
        Transaction tx = Transaction.initiate(
            UserId.generate(),
            UserId.generate(),
            Money.of(100, Currency.EUR),
            PaymentMethod.CARD,
            fixedClock
        );

        // When
        tx.complete("external-tx-123", fixedClock);

        // Then
        assertThat(tx.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(tx.completedAt()).isPresent();
        assertThat(tx.externalTransactionId()).hasValue("external-tx-123");
    }

    @Test
    void should_throw_exception_when_completing_already_completed() {
        // Given
        Transaction tx = Transaction.initiate(
            UserId.generate(),
            UserId.generate(),
            Money.of(100, Currency.EUR),
            PaymentMethod.CARD,
            fixedClock
        );
        tx.complete("tx-1", fixedClock);

        // When / Then
        assertThatThrownBy(() -> 
            tx.complete("tx-2", fixedClock)
        )
        .isInstanceOf(Transaction.TransactionAlreadyCompletedException.class);
    }

    @Test
    void should_fail_transaction() {
        // Given
        Transaction tx = Transaction.initiate(
            UserId.generate(),
            UserId.generate(),
            Money.of(100, Currency.EUR),
            PaymentMethod.CARD,
            fixedClock
        );

        // When
        tx.fail(fixedClock);

        // Then
        assertThat(tx.status()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.completedAt()).isPresent();
    }
}
```

---

### 🚀 Sprint 3-4 : Social Domain (Cercles de Vie)

```
domain/social/
├── model/
│   ├── Circle.java                   # Aggregate Root
│   ├── CircleId.java
│   ├── CircleType.java               # Enum (FAMILY, WORK, FRIENDS, PROJECT)
│   ├── CircleMembership.java         # Entity
│   ├── CircleRule.java               # VO (notification, visibility)
│   └── VisibilityLevel.java          # Enum
├── port/
│   └── CircleRepository.java
└── factory/
    └── CircleFactory.java

application/social/
├── CreateCircleUseCase.java
├── AddMemberToCircleUseCase.java
├── RemoveMemberFromCircleUseCase.java
├── UpdateCircleRulesUseCase.java
└── SuggestCircleForContactUseCase.java  # IA
```

**Temps estimé**: 2 sprints (4 semaines)

---

### 🚀 Sprint 5-6 : Workspace & Time Tracking

```
domain/workspace/
├── model/
│   ├── Task.java
│   ├── TaskStatus.java
│   ├── TimeEntry.java
│   ├── Project.java
│   └── Kanban.java
├── port/
│   ├── TaskRepository.java
│   └── TimeEntryRepository.java

domain/invoice/
├── model/
│   ├── Invoice.java
│   ├── InvoiceItem.java
│   ├── InvoiceStatus.java
│   └── TaxRate.java
```

**Temps estimé**: 2 sprints (4 semaines)

---

### 🚀 Sprint 7-8 : Assistant IA & Wellbeing

```
domain/assistant/
├── model/
│   ├── Reminder.java
│   ├── Task.java
│   └── ContextualSuggestion.java
├── port/
│   ├── AIAnalysisPort.java           # NLP, Intent Detection
│   └── CalendarPort.java

domain/wellbeing/
├── model/
│   ├── UsageSession.java
│   ├── DigitalHealthMetrics.java
│   ├── FocusSession.java
│   └── BreakSuggestion.java
```

**Temps estimé**: 2 sprints (4 semaines)

---

### 🚀 Sprint 9-10 : Features Avancées Messagerie

```
domain/message/
├── port/
│   ├── VoiceTranscriptionPort.java
│   ├── TranslationPort.java
│   ├── IntentDetectionPort.java
│   └── EmotionAnalysisPort.java
├── service/
│   ├── IntentDetectionService.java
│   ├── MessageEnrichmentService.java
│   └── ConversationSummaryService.java

application/message/
├── TranscribeVoiceMessageUseCase.java
├── TranslateMessageUseCase.java
├── DetectMessageIntentUseCase.java
├── SummarizeConversationUseCase.java
└── SearchMessagesByIntentUseCase.java
```

**Temps estimé**: 2 sprints (4 semaines)

---

### 🚀 Sprint 11-12 : Complétion & Polish

- ✅ Features QR Code avancées (géofencing, contexte IA)
- ✅ Event Sourcing + Domain Events
- ✅ CQRS léger (Read Models optimisés)
- ✅ Tests d'intégration complets
- ✅ Performance testing
- ✅ Documentation technique complète

---

## 🧪 STRATÉGIE DE TESTS

### Pour chaque nouveau domaine :

1. **Tests Unitaires Domain** (sans Spring)
   ```java
   // domain/test/payment/TransactionTest.java
   // PAS de @SpringBootTest
   // JUSTE des POJOs + TimeProvider mock
   ```

2. **Tests Use Cases** (sans Spring)
   ```java
   // application/test/payment/SendPaymentUseCaseTest.java
   // Mock des repositories
   // Vérification orchestration
   ```

3. **Tests Intégration Infrastructure**
   ```java
   // infrastructure/test/payment/TransactionRepositoryAdapterTest.java
   // @DataJpaTest
   // Vérifier mapping JPA
   ```

4. **Tests End-to-End**
   ```java
   // bootstrap/test/PaymentControllerE2ETest.java
   // @SpringBootTest
   // Appels HTTP complets
   ```

### Couverture cible :
- Domain : **95%+**
- Application : **90%+**
- Infrastructure : **80%+**
- Global : **85%+**

---

## 📊 MÉTRIQUES QUALITÉ À MAINTENIR

### Architecture

```bash
# ✅ Domain doit compiler sans dépendances externes
cd domain && mvn clean compile
# Doit réussir sans Spring/JPA

# ✅ Tests domain sans Spring
cd domain && mvn test
# Aucun @SpringBootTest

# ✅ Analyse dépendances
mvn dependency:analyze
# domain/ doit avoir 0 dépendances compile
```

### Code Quality

```bash
# ✅ Checkstyle (Google Java Style)
mvn checkstyle:check

# ✅ SpotBugs (sécurité)
mvn spotbugs:check

# ✅ Coverage Jacoco
mvn jacoco:report
# Target : 85%+
```

### Invariants DDD

- ❌ Interdire `@Builder` (Lombok)
- ❌ Interdire `@Setter` (Lombok)
- ❌ Interdire `LocalDate.now()`, `Instant.now()`
- ✅ Utiliser `TimeProvider.now()`
- ✅ Constructeurs privés
- ✅ Factory methods explicites
- ✅ Immutabilité (final fields)

---

## 🎯 CHECKLIST VALIDATION CONFORMITÉ

### Pour chaque Pull Request :

- [ ] **Domain** : 0 dépendance externe
- [ ] **Aggregate** : Invariants protégés
- [ ] **Factory** : Factory method au lieu de constructeur public
- [ ] **TimeProvider** : Injecté, jamais `now()` statique
- [ ] **Tests Domain** : Sans Spring
- [ ] **Tests Use Case** : Sans Spring
- [ ] **Value Objects** : Record immutable
- [ ] **Ports** : Interfaces dans domain
- [ ] **Adapters** : Implémentation dans infrastructure
- [ ] **Pas de Lombok** : Ni @Builder, ni @Setter

---

## 📚 TEMPLATES RÉUTILISABLES

### Aggregate Root Template

```java
package com.uzima.domain.<module>.model;

import com.uzima.domain.shared.TimeProvider;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root : <Nom>.
 * 
 * Invariants :
 * - ...
 * - ...
 */
public final class <Nom> {

    private final <Nom>Id id;
    // final fields pour immutabilité
    private final ...;
    
    // États mutables (si nécessaire)
    private ...;

    // Constructeur PRIVÉ
    private <Nom>(...) {
        // Validation invariants
        Objects.requireNonNull(...);
        if (...) {
            throw new <BusinessRuleException>(...);
        }
        
        this.id = id;
        ...
    }

    // Factory method PUBLIC
    public static <Nom> create(..., TimeProvider clock) {
        Objects.requireNonNull(clock, "TimeProvider obligatoire");
        return new <Nom>(
            <Nom>Id.generate(),
            ...,
            clock.now()
        );
    }

    // Reconstitution depuis persistence
    public static <Nom> reconstitute(...) {
        <Nom> entity = new <Nom>(...);
        // Restaurer état mutable
        return entity;
    }

    // Comportements métier
    public void doSomething(..., TimeProvider clock) {
        // Validation règles métier
        if (!canDoSomething()) {
            throw new <Exception>("...");
        }
        
        // Mutation état
        this.state = ...;
        
        // TODO: Lever DomainEvent
    }

    // Accesseurs (pas de setters)
    public <Nom>Id id() { return id; }
    public ... get...() { return ...; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof <Nom> n)) return false;
        return id.equals(n.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    // Exceptions domaine
    public static final class <BusinessRuleException> extends DomainException {
        public <BusinessRuleException>(String msg) { super(msg); }
    }
}
```

### Use Case Template

```java
package com.uzima.application.<module>;

import com.uzima.application.<module>.port.in.<Command>;
import com.uzima.application.<module>.port.out.*;
import com.uzima.domain.<module>.model.*;
import com.uzima.domain.<module>.port.*;
import com.uzima.domain.shared.TimeProvider;
import java.util.Objects;

/**
 * Use Case : <Description>.
 * 
 * Orchestration :
 * 1. ...
 * 2. ...
 */
public class <Nom>UseCase {

    private final <Repository> repository;
    private final <Port> externalPort;
    private final TimeProvider clock;

    public <Nom>UseCase(
            <Repository> repository,
            <Port> externalPort,
            TimeProvider clock
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.externalPort = Objects.requireNonNull(externalPort);
        this.clock = Objects.requireNonNull(clock);
    }

    public <ResultId> execute(<Command> command) {
        Objects.requireNonNull(command);

        // 1. Validation input
        // ...

        // 2. Charger aggregates si nécessaire
        // ...

        // 3. Comportement domaine
        <Aggregate> aggregate = <Aggregate>.create(..., clock);

        // 4. Persister
        repository.save(aggregate);

        // 5. Appeler services externes si nécessaire
        // externalPort.doSomething(...);

        // TODO: Publier DomainEvents

        return aggregate.id();
    }
}
```

---

## 🏁 CONCLUSION

### Résumé du Plan

**Durée totale** : 6 mois (12 sprints x 2 semaines)

**Effort** :
- 3-4 développeurs seniors DDD
- Revues architecture hebdomadaires
- Pair programming sur aggregates complexes

**Livrables** :
- ✅ 5 nouveaux domaines métier
- ✅ 45 nouveaux use cases
- ✅ 100% conformité DDD/Clean/Hexa
- ✅ Tests > 85% coverage
- ✅ Documentation complète

**Architecture finale** :
```
✅ 100% fonctionnalités PDF implémentées
✅ Architecture exemplaire maintenue
✅ Scalable et maintenable
✅ Prêt pour évolutions futures
```

**Prochaine étape** : Valider roadmap et démarrer Sprint 1 (Payment Domain)

---

**Document créé le** : 2026-03-12  
**Auteur** : Architecte Uzima Backend  
**Version** : 1.0
