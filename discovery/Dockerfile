# ------------ Build stage ------------
FROM maven:3.8.4-openjdk-17-slim AS builder
ARG APP_DIR=discovery
WORKDIR /app

# Copie des fichiers Maven
COPY ${APP_DIR}/pom.xml .
RUN mvn dependency:go-offline

# Copie des sources
COPY ${APP_DIR}/src ./src

# Compilation du projet avec encodage UTF-8
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8 -Dmaven.resources.skip=false -Dmaven.resources.filtering=false

# ------------ Run stage ------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crée l'utilisateur spring
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /app && \
    chown -R spring:spring /app

# Copie du JAR généré
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Passage à l'utilisateur spring
USER spring:spring

# Healthcheck via Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health/liveness || exit 1

# Optimisations mémoire + GC
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", \
    "app.jar"]
