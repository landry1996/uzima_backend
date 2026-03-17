# Référence API — Uzima

**Base URL** : `http://localhost:8080/api`
**Format** : JSON (`Content-Type: application/json`)
**Auth** : `Authorization: Bearer <access_token>` (sauf endpoints publics)

Documentation interactive : `http://localhost:8080/swagger-ui.html`
Spécification OpenAPI JSON : `http://localhost:8080/v3/api-docs`

---

## Sommaire

- [Authentification](#authentification)
- [Messages & IA](#messages--ia)
- [QR Codes](#qr-codes)
- [Paiements](#paiements)
- [Factures](#factures)
- [Workspace (Projets / Tâches)](#workspace-projets--tâches)
- [Cercles de Vie](#cercles-de-vie)
- [Assistant IA (Rappels)](#assistant-ia-rappels)
- [Bien-être numérique](#bien-être-numérique)
- [Codes d'erreur](#codes-derreur)

---

## Authentification

### Inscription

```http
POST /api/auth/register
```

**Body :**
```json
{
  "phoneNumber": "+2250700000000",
  "name": "Kofi Mensah",
  "password": "motDePasse123"
}
```

**Réponse 201 :**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "phoneNumber": "+2250700000000",
  "name": "Kofi Mensah",
  "presenceStatus": "AVAILABLE"
}
```

---

### Connexion

```http
POST /api/auth/login
```

**Body :**
```json
{
  "phoneNumber": "+2250700000000",
  "password": "motDePasse123"
}
```

**Réponse 200 :**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "d3a8f2c1...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

**Erreurs :**
| Code | Raison |
|------|--------|
| 401 | Identifiants invalides |
| 423 | Compte verrouillé (brute force) |
| 429 | Trop de tentatives (rate limiting IP) |

---

### Renouvellement du token

```http
POST /api/auth/token/refresh
```

**Body :**
```json
{ "refreshToken": "d3a8f2c1..." }
```

**Réponse 200 :**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "a9b1e3f7...",
  "expiresIn": 900
}
```

---

### Révocation du token

```http
DELETE /api/auth/token/revoke
Authorization: Bearer <access_token>
```

**Body :**
```json
{ "refreshToken": "d3a8f2c1..." }
```

**Réponse 204** (No Content)

---

## Messages & IA

### Envoyer un message

```http
POST /api/messages
Authorization: Bearer <token>
```

**Body :**
```json
{
  "conversationId": "uuid",
  "senderId": "uuid",
  "type": "TEXT",
  "content": "Bonjour !"
}
```

Types disponibles : `TEXT`, `VOICE`, `IMAGE`, `VIDEO`, `FILE`, `LOCATION`, `PAYMENT_REQUEST`

**Réponse 201 :**
```json
{ "id": "uuid" }
```

---

### Récupérer les messages

```http
GET /api/messages?conversationId=<uuid>
Authorization: Bearer <token>
```

---

### Transcrire un message vocal

```http
POST /api/messages/{id}/transcribe
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "messageId": "uuid",
  "transcription": "Bonjour, est-ce que tu es disponible ?",
  "detectedLanguage": "fr"
}
```

---

### Traduire un message

```http
POST /api/messages/{id}/translate?targetLanguage=en
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "messageId": "uuid",
  "translation": "Hello, are you available?",
  "targetLanguage": "en"
}
```

---

### Détecter l'intention d'un message

```http
POST /api/messages/{id}/detect-intent
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "messageId": "uuid",
  "detectedIntent": "payment_request"
}
```

Intentions reconnues : `payment_request`, `meeting_scheduling`, `task_assignment`, `emergency`, `greeting`, `unknown`

---

### Analyser l'émotion vocale

```http
POST /api/messages/{id}/analyze-emotion
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "messageId": "uuid",
  "detectedEmotion": "neutral"
}
```

---

### Résumé de conversation

```http
GET /api/conversations/{id}/summary?language=fr
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "conversationId": "uuid",
  "summary": "Discussion autour d'un projet freelance..."
}
```

---

### Recherche par intention

```http
GET /api/conversations/{id}/messages/search?intent=payment_request
Authorization: Bearer <token>
```

**Réponse 200 :** Liste de messages filtrés par intention détectée.

---

## QR Codes

### Créer un QR Code

```http
POST /api/qrcodes
Authorization: Bearer <token>
```

**Body :**
```json
{
  "ownerId": "uuid",
  "type": "PROFESSIONAL",
  "validForMinutes": 1440,
  "singleUse": false
}
```

Types disponibles : `PROFESSIONAL`, `SOCIAL`, `PAYMENT`, `TEMPORARY_LOCATION`, `EVENT`, `MEDICAL_EMERGENCY`

> `TEMPORARY_LOCATION` et `EVENT` exigent `validForMinutes` non null.

**Réponse 201 :**
```json
{ "id": "uuid" }
```

---

### Lister mes QR Codes

```http
GET /api/qrcodes?ownerId=<uuid>
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
[
  {
    "id": "uuid",
    "ownerId": "uuid",
    "type": "PROFESSIONAL",
    "status": "ACTIVE",
    "createdAt": "2026-03-13T10:00:00Z",
    "scanCount": 3,
    "hasGeofence": true
  }
]
```

---

### Scanner un QR Code

```http
POST /api/qrcodes/{id}/scan?scannerId=<uuid>
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "qrCodeId": "uuid",
  "type": "PROFESSIONAL",
  "ownerId": "uuid",
  "totalScans": 4
}
```

**Erreurs :**
| Code | Raison |
|------|--------|
| 403 | Scanner hors de la zone géographique |
| 400 | Position GPS non disponible (QR géofencé) |
| 422 | QR Code révoqué / expiré / limite de scans atteinte |

---

### Révoquer un QR Code

```http
POST /api/qrcodes/{id}/revoke?requesterId=<uuid>
Authorization: Bearer <token>
```

**Réponse 204** (No Content)

---

### Configurer les règles (géofencing + personnalisation)

```http
PUT /api/qrcodes/{id}/rules?requesterId=<uuid>
Authorization: Bearer <token>
```

**Body :**
```json
{
  "geofence": {
    "latitude": 5.3600,
    "longitude": -4.0083,
    "radiusMeters": 200
  },
  "personalization": {
    "condition": "WORK_HOURS",
    "targetProfile": "COLLEAGUE"
  }
}
```

Les champs `geofence` et `personalization` sont optionnels. Passer `null` supprime la règle existante.

**Réponse 204** (No Content)

---

### Suggestion de type contextuel

```http
GET /api/qrcodes/suggest?userId=<uuid>
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "suggestedType": "PROFESSIONAL",
  "reason": "Événement professionnel actif dans votre calendrier"
}
```

Logique de suggestion :
1. Calendrier → `PROFESSIONAL` si événement professionnel, `EVENT` sinon
2. Heure : 8h–18h → `PROFESSIONAL`, sinon → `SOCIAL`

---

## Paiements

### Envoyer un paiement

```http
POST /api/payments/send
Authorization: Bearer <token>
```

**Body :**
```json
{
  "senderId": "uuid",
  "recipientId": "uuid",
  "amount": "5000.00",
  "currency": "XOF",
  "method": "MOBILE_MONEY",
  "description": "Remboursement déjeuner"
}
```

Devises : `EUR`, `USD`, `XOF`, `XAF`, `GHS`, `NGN`
Méthodes : `MOBILE_MONEY`, `CARD`, `CRYPTO`, `WALLET`

**Réponse 201 :**
```json
{ "transactionId": "uuid" }
```

---

### Demander un paiement

```http
POST /api/payments/request
Authorization: Bearer <token>
```

**Body :**
```json
{
  "requesterId": "uuid",
  "payerId": "uuid",
  "amount": "2500.00",
  "currency": "XOF",
  "description": "Part du loyer"
}
```

---

### Historique des transactions

```http
GET /api/payments/history?userId=<uuid>&limit=20&offset=0
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "sent": [...],
  "received": [...],
  "totalSent": "15000.00",
  "totalReceived": "8500.00",
  "total": 12
}
```

---

### Annuler une transaction

```http
DELETE /api/payments/{id}?requesterId=<uuid>
Authorization: Bearer <token>
```

**Réponse 204** (No Content — uniquement si statut `PENDING`)

---

## Factures

### Créer une facture

```http
POST /api/invoices
Authorization: Bearer <token>
```

**Body :**
```json
{
  "issuerId": "uuid",
  "clientId": "uuid",
  "currency": "XOF",
  "items": [
    {
      "description": "Développement mobile",
      "quantity": 5,
      "unitPrice": "50000.00",
      "taxRateCode": "TVA_18"
    }
  ]
}
```

Codes de taxe : `EXEMPT`, `TVA_18`, `TVA_19_25`, `TVA_20`, `VAT_7_5`

**Réponse 201 :**
```json
{ "invoiceId": "uuid" }
```

---

### Liste des factures

```http
GET /api/invoices?issuerId=<uuid>
Authorization: Bearer <token>
```

---

### Détail d'une facture

```http
GET /api/invoices/{id}
Authorization: Bearer <token>
```

---

### Envoyer une facture (DRAFT → SENT)

```http
POST /api/invoices/{id}/send?requesterId=<uuid>
Authorization: Bearer <token>
```

**Réponse 204**

---

### Marquer comme payée (SENT → PAID)

```http
POST /api/invoices/{id}/paid?requesterId=<uuid>
Authorization: Bearer <token>
```

**Réponse 204**

---

### Annuler une facture

```http
POST /api/invoices/{id}/cancel?requesterId=<uuid>
Authorization: Bearer <token>
```

**Réponse 204**

---

## Workspace (Projets / Tâches)

### Créer un projet

```http
POST /api/projects
Authorization: Bearer <token>
```

**Body :**
```json
{
  "name": "Refonte site e-commerce",
  "description": "Migration vers Next.js",
  "ownerId": "uuid"
}
```

**Réponse 201 :**
```json
{ "projectId": "uuid" }
```

---

### Tableau Kanban

```http
GET /api/projects/{id}/kanban
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "projectId": "uuid",
  "columns": {
    "BACKLOG": [...],
    "IN_PROGRESS": [...],
    "IN_REVIEW": [...],
    "DONE": [...]
  }
}
```

---

### Créer une tâche

```http
POST /api/projects/{id}/tasks
Authorization: Bearer <token>
```

**Body :**
```json
{
  "title": "Intégration paiement Stripe",
  "description": "...",
  "priority": "HIGH",
  "assigneeId": "uuid",
  "requesterId": "uuid"
}
```

Priorités : `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

---

### Mettre à jour le statut d'une tâche

```http
PUT /api/projects/{projectId}/tasks/{taskId}/status
Authorization: Bearer <token>
```

**Body :**
```json
{
  "newStatus": "IN_PROGRESS",
  "requesterId": "uuid"
}
```

Transitions valides : `BACKLOG→IN_PROGRESS`, `IN_PROGRESS→IN_REVIEW`, `IN_REVIEW→DONE`, `IN_REVIEW→IN_PROGRESS` (reject), `DONE→BACKLOG` (reopen), `*→BLOCKED`

---

### Démarrer un time entry

```http
POST /api/projects/{id}/time-entries/start
Authorization: Bearer <token>
```

**Body :**
```json
{
  "taskId": "uuid",
  "userId": "uuid"
}
```

---

### Rapport de temps

```http
GET /api/projects/{id}/time-report
Authorization: Bearer <token>
```

---

## Cercles de Vie

Les Cercles de Vie permettent de cloisonner les contacts en groupes contextuels (Famille, Travail, Amis, Projets).

### Créer un cercle

```http
POST /api/circles
Authorization: Bearer <token>
```

**Body :**
```json
{
  "name": "Équipe backend",
  "type": "WORK",
  "ownerId": "uuid",
  "visibilityLevel": "MEMBERS_ONLY"
}
```

Types : `FAMILY`, `WORK`, `FRIENDS`, `PROJECTS`
Visibilité : `PUBLIC`, `MEMBERS_ONLY`, `PRIVATE`

---

### Mes cercles

```http
GET /api/circles?userId=<uuid>
Authorization: Bearer <token>
```

---

### Ajouter un membre

```http
POST /api/circles/{id}/members
Authorization: Bearer <token>
```

**Body :**
```json
{
  "requesterId": "uuid",
  "userId": "uuid",
  "role": "MEMBER"
}
```

Rôles : `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`

---

### Retirer un membre

```http
DELETE /api/circles/{id}/members/{userId}?requesterId=<uuid>
Authorization: Bearer <token>
```

**Réponse 204**

---

### Mettre à jour les règles

```http
PUT /api/circles/{id}/rules
Authorization: Bearer <token>
```

**Body :**
```json
{
  "requesterId": "uuid",
  "notificationPolicy": "URGENT_ONLY",
  "allowMessageRequests": false,
  "allowLocationSharing": true
}
```

Politiques : `ALL_MESSAGES`, `URGENT_ONLY`, `NONE`

---

### Suggestion de cercle

```http
GET /api/circles/suggest?requesterId=<uuid>&targetUserId=<uuid>
Authorization: Bearer <token>
```

---

## Assistant IA (Rappels)

### Créer un rappel

```http
POST /api/assistant/reminders
Authorization: Bearer <token>
```

**Body :**
```json
{
  "userId": "uuid",
  "content": "Appeler le client avant 17h",
  "trigger": "TIME_BASED",
  "scheduledAt": "2026-03-14T16:00:00Z"
}
```

Triggers : `TIME_BASED`, `LOCATION_BASED`, `CONTEXT_BASED`

**Réponse 201 :**
```json
{ "reminderId": "uuid" }
```

---

### Liste des rappels

```http
GET /api/assistant/reminders?userId=<uuid>&status=PENDING
Authorization: Bearer <token>
```

Statuts filtrables : `PENDING`, `TRIGGERED`, `SNOOZED`, `DISMISSED`

---

### Rappels actifs

```http
GET /api/assistant/reminders/active?userId=<uuid>
Authorization: Bearer <token>
```

Retourne les rappels en statut `PENDING` ou `SNOOZED`.

---

### Snoozer un rappel

```http
POST /api/assistant/reminders/{id}/snooze
Authorization: Bearer <token>
```

**Body :**
```json
{
  "userId": "uuid",
  "delayMinutes": 15
}
```

**Réponse 204**

---

### Rejeter un rappel

```http
POST /api/assistant/reminders/{id}/dismiss
Authorization: Bearer <token>
```

**Body :**
```json
{ "userId": "uuid" }
```

**Réponse 204**

---

## Bien-être numérique

### Rapport de santé digitale

```http
GET /api/wellbeing/report?userId=<uuid>&from=2026-03-06&to=2026-03-13
Authorization: Bearer <token>
```

**Réponse 200 :**
```json
{
  "userId": "uuid",
  "periodStart": "2026-03-06",
  "periodEnd": "2026-03-13",
  "healthScore": 74,
  "summary": "Bon équilibre numérique",
  "totalScreenTimeMinutes": 3240,
  "productiveTimeMinutes": 2100,
  "focusSessionCount": 12,
  "totalFocusTimeMinutes": 480,
  "interruptedFocusCount": 2,
  "timeByCategory": {
    "WORK": 2100,
    "COMMUNICATION": 840,
    "SOCIAL_MEDIA": 180,
    "ENTERTAINMENT": 120
  }
}
```

Score de santé : 0–100. Seuil "sain" = 60.

---

### Démarrer une session Focus

```http
POST /api/wellbeing/focus
Authorization: Bearer <token>
```

**Body :**
```json
{ "userId": "uuid" }
```

**Réponse 201 :**
```json
{ "sessionId": "uuid" }
```

> Erreur 409 si une session Focus est déjà active pour cet utilisateur.

---

### Terminer une session Focus

```http
POST /api/wellbeing/focus/{id}/end
Authorization: Bearer <token>
```

**Body :**
```json
{ "userId": "uuid" }
```

**Réponse 204**

---

### Interrompre une session Focus

```http
POST /api/wellbeing/focus/{id}/interrupt
Authorization: Bearer <token>
```

**Body :**
```json
{
  "userId": "uuid",
  "reason": "NOTIFICATION"
}
```

Raisons : `NOTIFICATION`, `CALL`, `MEETING`, `DISTRACTION`, `OTHER`

**Réponse 204**

---

### Enregistrer l'usage d'une application

```http
POST /api/wellbeing/usage
Authorization: Bearer <token>
```

**Body :**
```json
{
  "userId": "uuid",
  "appName": "VS Code",
  "appType": "WORK"
}
```

Types d'app : `WORK`, `COMMUNICATION`, `SOCIAL_MEDIA`, `ENTERTAINMENT`, `HEALTH`, `EDUCATION`, `OTHER`

**Réponse 201 :**
```json
{ "sessionId": "uuid" }
```

---

## Codes d'erreur

Format standard des erreurs :

```json
{
  "status": 422,
  "errorCode": "QR_CODE_REVOKED",
  "message": "Le QR Code a été révoqué et ne peut plus être scanné",
  "timestamp": "2026-03-13T10:30:00Z"
}
```

### Table des codes d'erreur

| HTTP | errorCode | Cause |
|------|-----------|-------|
| 400 | `VALIDATION_ERROR` | Champs invalides (@Valid) |
| 400 | `EXPIRATION_REQUIRED` | Type QR exige une date d'expiration |
| 400 | `GEOLOCATION_UNAVAILABLE` | GPS non disponible pour QR géofencé |
| 400 | `INVALID_GEOFENCE` | Coordonnées / rayon invalides |
| 400 | `INVALID_PERSONALIZATION_RULE` | Condition ou profil vide |
| 400 | `INVALID_REMINDER_CONTENT` | Contenu rappel vide ou trop long |
| 400 | `INVALID_APP_NAME` | Nom d'application vide ou trop long |
| 401 | `UNAUTHORIZED` | Token absent ou invalide |
| 403 | `ACCESS_DENIED` | Opération non autorisée |
| 403 | `OUTSIDE_GEOFENCE` | Scanner hors de la zone autorisée |
| 404 | `RESOURCE_NOT_FOUND` | Ressource inexistante |
| 409 | `CONFLICT` | Conflit d'état (ex: session Focus déjà active) |
| 422 | `QR_CODE_REVOKED` | QR Code révoqué |
| 422 | `QR_CODE_EXPIRED` | QR Code expiré |
| 422 | `SCAN_LIMIT_REACHED` | Limite de scans atteinte |
| 422 | `ILLEGAL_REMINDER_TRANSITION` | Transition d'état invalide (ex: snooze sur DISMISSED) |
| 422 | `FOCUS_SESSION_ALREADY_ENDED` | Session Focus déjà terminée / interrompue |
| 422 | `ILLEGAL_TRANSITION` | Transition d'état invalide (Transaction, Task, Invoice) |
| 423 | `ACCOUNT_LOCKED` | Compte verrouillé (brute force) |
| 429 | `RATE_LIMIT_EXCEEDED` | Trop de requêtes depuis cette IP |
| 500 | `INTERNAL_ERROR` | Erreur serveur inattendue |
