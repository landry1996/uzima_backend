# Guide d'Installation — Uzima

## Sommaire

- [Prérequis](#prérequis)
- [Installation rapide (Docker)](#installation-rapide-docker)
- [Installation manuelle](#installation-manuelle)
- [Variables d'environnement](#variables-denvironnement)
- [Lancer le backend](#lancer-le-backend)
- [Lancer le frontend mobile](#lancer-le-frontend-mobile)
- [Vérification de l'installation](#vérification-de-linstallation)
- [Commandes utiles](#commandes-utiles)
- [Troubleshooting](#troubleshooting)
- [Déploiement production](#déploiement-production)

---

## Prérequis

### Backend

| Outil | Version | Vérification |
|-------|---------|-------------|
| Java JDK | **25** | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| PostgreSQL | 15+ | `psql --version` |
| Docker (optionnel) | 24+ | `docker --version` |

> Le projet utilise Java 25. Assurez-vous que `JAVA_HOME` pointe vers un JDK 25.

### Mobile

| Outil | Version |
|-------|---------|
| Node.js | 18+ |
| npm | 9+ |
| React Native CLI | dernière |
| Android Studio | Hedgehog+ (pour Android) |
| Xcode | 15+ (iOS, Mac uniquement) |

---

## Installation rapide (Docker)

La méthode la plus simple pour le développement local.

```bash
# 1. Cloner le projet
git clone <repo-url>
cd uzima

# 2. Démarrer PostgreSQL + Redis
docker-compose up -d

# 3. Configurer les variables d'environnement
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=uzima-dev-secret-key-CHANGE-IN-PRODUCTION-32ch

# 4. Compiler et lancer le backend
cd backend
mvn clean package -DskipTests
mvn spring-boot:run -pl bootstrap
```

Le backend démarre sur `http://localhost:8080`.
Les migrations Flyway (V1–V8) s'exécutent automatiquement au premier démarrage.

---

## Installation manuelle

### 1. PostgreSQL

**Ubuntu / Debian :**
```bash
sudo apt update
sudo apt install postgresql-15
sudo systemctl start postgresql
```

**macOS (Homebrew) :**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Windows :**
Télécharger l'installeur depuis https://www.postgresql.org/download/windows/

### 2. Créer la base de données

```bash
psql -U postgres
```

```sql
CREATE DATABASE uzima;
CREATE USER uzima_user WITH ENCRYPTED PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE uzima TO uzima_user;
\q
```

### 3. Redis (optionnel — MVP utilise in-memory)

```bash
# Ubuntu
sudo apt install redis-server

# macOS
brew install redis && brew services start redis

# Docker
docker run -d -p 6379:6379 redis:7-alpine
```

> En développement, le rate limiting et le stockage des tentatives de connexion sont in-memory. Redis est requis uniquement pour une configuration multi-instances en production.

---

## Variables d'environnement

Créer un fichier `.env` à la racine ou exporter les variables :

```bash
# Base de données (obligatoire)
DB_USERNAME=postgres
DB_PASSWORD=votre_mot_de_passe

# JWT (obligatoire en production — minimum 32 caractères)
JWT_SECRET=uzima-secret-key-CHANGE-IN-PRODUCTION-minimum-32-chars
JWT_EXPIRATION_SECONDS=900    # 15 minutes (défaut)
```

Les variables sont référencées dans `backend/bootstrap/src/main/resources/application.yml` :

```yaml
spring:
  datasource:
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

uzima:
  security:
    jwt:
      secret: ${JWT_SECRET:uzima-dev-secret-key-CHANGE-IN-PRODUCTION-32ch}
      expiration-seconds: ${JWT_EXPIRATION_SECONDS:900}
```

> En production, ne jamais commit les vraies valeurs. Utiliser un gestionnaire de secrets (Vault, AWS Secrets Manager, variables d'environnement CI/CD).

---

## Lancer le backend

### Compilation

```bash
cd backend

# Compiler tous les modules
mvn clean compile

# Compiler + tests
mvn clean verify

# Compiler sans tests (rapide)
mvn clean package -DskipTests
```

### Lancement

```bash
# Via Spring Boot Maven Plugin
mvn spring-boot:run -pl bootstrap

# Via le JAR généré
java -jar bootstrap/target/uzima-bootstrap-1.0.0-SNAPSHOT.jar
```

### Vérification des modules individuels

```bash
# Tester le domain seul (sans Spring, très rapide)
mvn test -pl domain

# Tester l'application seule
mvn test -pl application

# Vérifier que domain compile sans dépendances externes
cd domain && mvn clean compile
```

---

## Lancer le frontend mobile

### Installation des dépendances

```bash
cd mobile
npm install
```

### Android

```bash
# Démarrer l'émulateur Android (ou connecter un appareil)
# Puis :
npm run android
```

### iOS (Mac uniquement)

```bash
cd ios && pod install && cd ..
npm run ios
```

### Variables d'environnement mobile

Créer `mobile/.env` :
```
API_URL=http://localhost:8080
WEBSOCKET_URL=http://localhost:8080
```

> Sur Android émulateur, utiliser `10.0.2.2` à la place de `localhost` pour accéder à l'hôte.

---

## Vérification de l'installation

### Backend opérationnel

```bash
# Health check
curl http://localhost:8080/actuator/health
# Réponse attendue : {"status":"UP"}

# Inscription d'un utilisateur test
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"+2250700000001","name":"Test User","password":"test1234"}'
# Réponse attendue : 201 avec l'id utilisateur

# Documentation API interactive
open http://localhost:8080/swagger-ui.html
```

### Migrations Flyway exécutées

```sql
-- Vérifier dans PostgreSQL
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
-- Doit retourner 8 lignes (V1 à V8), toutes avec success = true
```

---

## Commandes utiles

### Tests

```bash
# Tous les tests
mvn test

# Tests domain uniquement (ultra-rapide, sans Spring)
mvn test -pl domain

# Tests avec rapport de coverage (JaCoCo)
mvn verify -Pcoverage
# Rapport généré dans : target/site/jacoco/index.html
```

### Qualité

```bash
# Vérification des conventions de code
mvn checkstyle:check

# Recherche des violations des règles strictes
grep -r "Instant\.now()" backend/domain/src/main --include="*.java"
grep -r "@Builder\|@Setter" backend/domain/src/main --include="*.java"
# Les deux commandes doivent retourner 0 résultats
```

### Base de données

```bash
# Voir l'état des migrations Flyway
mvn flyway:info -pl bootstrap

# Réparer une migration échouée (dev uniquement)
mvn flyway:repair -pl bootstrap
```

### Build Docker (optionnel)

```bash
# Construire l'image
docker build -t uzima-backend:latest ./backend

# Lancer avec les variables d'environnement
docker run -p 8080:8080 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e JWT_SECRET=your-secret \
  uzima-backend:latest
```

---

## Troubleshooting

### Le backend ne démarre pas

**Erreur : `Connection refused` (PostgreSQL)**
```
Cause : PostgreSQL n'est pas lancé ou la config est incorrecte.
Solution :
  - docker-compose up -d
  - Vérifier DB_USERNAME, DB_PASSWORD
  - Vérifier que le port 5432 est libre : lsof -i :5432
```

**Erreur : `FlywayException: Validate failed`**
```
Cause : Les migrations ont été modifiées après exécution.
Solution (dev uniquement) :
  - mvn flyway:repair -pl bootstrap
  Ou : supprimer la table flyway_schema_history et relancer
```

**Erreur : `Java version mismatch`**
```
Cause : JAVA_HOME ne pointe pas vers JDK 25.
Solution :
  - export JAVA_HOME=/path/to/jdk-25
  - java -version  # doit afficher 25
```

**Erreur : `JWT_SECRET too short`**
```
Cause : La clé JWT fait moins de 32 caractères.
Solution : Utiliser une clé d'au moins 32 caractères.
  - export JWT_SECRET=$(openssl rand -hex 32)
```

### Les tests échouent

**Erreur : `NoSuchBeanDefinitionException`**
```
Cause : Tests unitaires domain/application ne doivent PAS utiliser Spring.
Solution : Vérifier que les tests n'ont pas @SpringBootTest.
  Les tests domain utilisent uniquement JUnit5 + AssertJ + Mockito.
```

### Le frontend mobile ne compile pas

```bash
# Nettoyer le cache Metro
npm start -- --reset-cache

# Réinstaller les dépendances
rm -rf node_modules && npm install

# Android : nettoyer le build Gradle
cd android && ./gradlew clean && cd ..
```

---

## Déploiement production

### Checklist avant déploiement

- [ ] `JWT_SECRET` = clé aléatoire sécurisée (min 32 chars)
- [ ] `DB_PASSWORD` = mot de passe fort
- [ ] `spring.jpa.show-sql: false` dans application.yml
- [ ] Remplacer `FakePaymentGatewayAdapter` par un vrai gateway
- [ ] Remplacer `InMemoryLoginAttemptRepository` par `RedisLoginAttemptRepository`
- [ ] Remplacer les Stubs AI par les vrais adaptateurs
- [ ] Configurer CORS avec les vraies origines prod (`https://app.uzima.com`)
- [ ] Activer HTTPS (HSTS déjà configuré)
- [ ] Configurer le pool de connexions HikariCP
- [ ] Mettre en place les alertes de monitoring (Actuator + Grafana)

### Configuration production recommandée

```yaml
# application-prod.yml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

logging:
  level:
    com.uzima: INFO
    org.hibernate.SQL: WARN
```

Lancement en production :
```bash
java -jar uzima-bootstrap.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/etc/uzima/application-prod.yml
```
