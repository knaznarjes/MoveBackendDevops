# Build stage
FROM maven:3.8.4-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline

COPY src ./src
# Build the application
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create the spring user and set up permissions
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /app && \
    chown -R spring:spring /app

# Copy the jar file
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to spring user
USER spring:spring

# Expose Eureka port
EXPOSE 8761

# Health check for Eureka
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# JVM settings optimized for Discovery Service
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=70.0", \
    "-XX:+UseG1GC", \
    "-Dspring.profiles.active=docker", \
    "-jar", \
    "app.jar"]