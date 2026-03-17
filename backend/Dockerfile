# ==============================================================================
# Stage 1 — Build
# Compile le projet Maven multi-modules et produit le fat-jar Spring Boot.
# ==============================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# --- Copie des POM en premier pour exploiter le cache des dépendances ---
COPY pom.xml                  ./pom.xml
COPY domain/pom.xml           domain/pom.xml
COPY application/pom.xml      application/pom.xml
COPY security/pom.xml         security/pom.xml
COPY infrastructure/pom.xml   infrastructure/pom.xml
COPY bootstrap/pom.xml        bootstrap/pom.xml

# Téléchargement des dépendances (layer mis en cache tant que les POM ne changent pas)
RUN mvn dependency:go-offline -B --no-transfer-progress -q

# --- Copie des sources ---
COPY domain/src           domain/src
COPY application/src      application/src
COPY security/src         security/src
COPY infrastructure/src   infrastructure/src
COPY bootstrap/src        bootstrap/src

# Build complet — tests ignorés (ils tournent en CI)
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ==============================================================================
# Stage 2 — Runtime
# Image minimale : JRE seul, utilisateur non-root, aucun outil de build.
# ==============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

# Utilisateur non-root pour limiter la surface d'attaque
RUN addgroup -S uzima && adduser -S uzima -G uzima

WORKDIR /app

# Copie du fat-jar uniquement
COPY --from=builder --chown=uzima:uzima /build/bootstrap/target/*.jar app.jar

USER uzima

# 8080  — API REST (HTTP)
# 9092  — Netty-SocketIO (WebSocket temps réel, configurable via WEBSOCKET_PORT)
EXPOSE 8080 9092

# Healthcheck : interroge l'endpoint Actuator toutes les 30s
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
  "-jar", "app.jar"]
