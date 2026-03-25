# 🚀 GUIDE INSTALLATION UZIMA MVP - IntelliJ IDEA

## ✅ Prérequis

### 1. Java Development Kit 21
- Télécharger : https://www.oracle.com/java/technologies/downloads/#java21
- Ou via SDKMAN : `sdk install java 21-open`
- Vérifier : `java -version` (doit afficher 21.x.x)

### 2. IntelliJ IDEA
- Télécharger : https://www.jetbrains.com/idea/download/
- Version Community (gratuite) ou Ultimate

### 3. PostgreSQL 15+
- Windows : https://www.postgresql.org/download/windows/
- Mac : `brew install postgresql@15`
- Linux : `sudo apt install postgresql-15`

### 4. Node.js 18+ (pour le mobile)
- Télécharger : https://nodejs.org/
- Vérifier : `node -v` et `npm -v`

---

## 📦 Étape 1 : Extraire le Projet

1. Décompresser `uzima-mvp.zip`
2. Vous devriez avoir cette structure :
```
uzima-mvp/
├── backend/        ← Backend Spring Boot
├── mobile/         ← App React Native
├── docs/           ← Documentation
└── README.md
```

---

## 🔧 Étape 2 : Configuration PostgreSQL

### Créer la base de données

**Windows (via pgAdmin) :**
1. Ouvrir pgAdmin
2. Créer nouvelle database : `uzima`
3. Définir owner : `postgres`

**Mac/Linux (via terminal) :**
```bash
# Se connecter à PostgreSQL
psql -U postgres

# Créer la base
CREATE DATABASE uzima;

# Vérifier
\l

# Quitter
\q
```

### Tester la connexion
```bash
psql -U postgres -d uzima -h localhost
```
Mot de passe par défaut : `postgres` (à changer en production)

---

## 💻 Étape 3 : Ouvrir le Projet dans IntelliJ

### 3.1 Ouvrir le Backend

1. **Lancer IntelliJ IDEA**
2. `File` > `Open`
3. Sélectionner le dossier `uzima-mvp/backend`
4. Cliquer `OK`

### 3.2 Configuration Automatique

IntelliJ va automatiquement :
- ✅ Détecter le projet Gradle
- ✅ Télécharger les dépendances (peut prendre 2-5 min)
- ✅ Indexer le code

**Attendre la fin** : Barre de progression en bas à droite

### 3.3 Configurer le JDK

1. `File` > `Project Structure` (Ctrl+Alt+Shift+S)
2. **Project** :
   - **SDK** : Sélectionner Java 21 
     - Si absent : `Add SDK` > `Download JDK` > Version 21
   - **Language Level** : 21
3. Cliquer `Apply` puis `OK`

---

## ▶️ Étape 4 : Lancer l'Application

### Méthode 1 : Via Configuration Run (Recommandé)

1. `Run` > `Edit Configurations...`
2. Cliquer `+` (Add New Configuration)
3. Sélectionner `Spring Boot`
4. Remplir :
   - **Name** : `UzimaApplication`
   - **Main class** : `com.uzima.UzimaApplication` 
     (cliquer sur `...` et chercher)
   - **Use classpath of module** : `uzima-backend.main`
5. Cliquer `OK`
6. Cliquer sur le bouton ▶️ **Run** (ou Shift+F10)

### Méthode 2 : Via Terminal IntelliJ

1. Ouvrir terminal IntelliJ (Alt+F12 ou en bas)
2. Exécuter :
```bash
# Mac/Linux
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

### Méthode 3 : Double-clic sur Main

1. Ouvrir `src/main/java/com/uzima/UzimaApplication.java`
2. Clic droit sur la classe
3. `Run 'UzimaApplication.main()'`

---

## ✅ Étape 5 : Vérifier que ça Marche

### Console IntelliJ doit afficher :
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::       (v3.2.1)

2026-02-18 ... : Started UzimaApplication in 3.456 seconds
```

### Tester l'API

**Dans navigateur :**
```
http://localhost:8080/api/messages
```
Devrait afficher : `{"messages": []}`

**Ou via terminal :**
```bash
curl http://localhost:8080/api/messages
```

---

## 📱 Étape 6 : Lancer l'App Mobile (Optionnel)

### Installation
```bash
cd mobile
npm install
```

### Android
```bash
npm run android
```
Prérequis : Android Studio + émulateur Android

### iOS (Mac uniquement)
```bash
cd ios
pod install
cd ..
npm run ios
```
Prérequis : Xcode + simulateur iOS

---

## 🐳 Alternative : Docker (Plus Simple)

Si vous avez Docker installé :

```bash
# Lancer PostgreSQL + Redis
docker-compose up -d

# Vérifier
docker ps
```

Puis lancer le backend normalement dans IntelliJ.

---

## 🔍 Troubleshooting

### ❌ Erreur : "Cannot resolve symbol 'SpringBootApplication'"

**Solution** : Recharger le projet Gradle
1. Clic droit sur `build.gradle`
2. `Reload Gradle Project`
3. Ou icône 🔄 dans l'onglet Gradle (à droite)

### ❌ Erreur : "Connection refused" PostgreSQL

**Solution** : Vérifier que PostgreSQL est lancé
```bash
# Mac
brew services list

# Linux
sudo systemctl status postgresql

# Windows
Vérifier dans Services (services.msc)
```

### ❌ Erreur : "Could not resolve dependencies"

**Solution** : Problème réseau / proxy
1. `File` > `Settings` > `Build, Execution, Deployment` > `Gradle`
2. Décocher `Offline work`
3. Tester connexion internet

### ❌ Port 8080 déjà utilisé

**Solution** : Changer le port
1. Éditer `src/main/resources/application.yml`
2. Modifier :
```yaml
server:
  port: 8081  # ou autre port libre
```

---

## 📚 Prochaines Étapes

Une fois le backend lancé :

1. ✅ Tester les endpoints : Voir `docs/API.md`
2. 🎨 Développer les features : Voir `docs/ARCHITECTURE.md`
3. 🧪 Écrire des tests : `src/test/java/com/uzima/`
4. 📱 Connecter le mobile : Voir `mobile/README.md`

---

## 🆘 Besoin d'Aide ?

- **Documentation backend** : `uzima-mvp/docs/`
- **Logs IntelliJ** : Onglet `Run` en bas
- **Logs PostgreSQL** : Vérifier dans pgAdmin ou terminal
- **Communauté Spring** : https://spring.io/guides

---

## 🎉 Félicitations !

Vous avez maintenant un **projet Spring Boot professionnel** prêt pour le développement !

**Structure du code :**
- `controller/` : Endpoints REST API
- `entity/` : Modèles de données (JPA)
- `repository/` : Accès base de données
- `service/` : Logique métier
- `config/` : Configuration Spring

**Commandes utiles :**
```bash
# Tests
./gradlew test

# Build
./gradlew build

# Clean
./gradlew clean

# Dépendances
./gradlew dependencies
```

🚀 **BON DÉVELOPPEMENT !**
