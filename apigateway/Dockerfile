# ------------ Build stage ------------
FROM maven:3.8.4-openjdk-17-slim AS builder
ARG APP_DIR=apigateway
WORKDIR /app

# Copie du fichier pom.xml
COPY ${APP_DIR}/pom.xml .
RUN mvn dependency:go-offline

# Copie du code source
COPY ${APP_DIR}/src ./src

# Compilation
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8 -Dmaven.resources.skip=false -Dmaven.resources.filtering=false

# ------------ Run stage ------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Création de l'utilisateur dédié
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /app && \
    chown -R spring:spring /app

# Copie du .jar
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Exécution sous l'utilisateur spring
USER spring:spring

# Health check (à adapter selon le port configuré dans application.yml)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Java optimisé pour containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", \
    "app.jar"]
