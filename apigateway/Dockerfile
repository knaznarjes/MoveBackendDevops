# Build stage
FROM maven:3.8.4-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline

COPY src ./src
# Skip resource filtering and use UTF-8 encoding
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8 -Dmaven.resources.skip=false -Dmaven.resources.filtering=false

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

# Expose Gateway port
EXPOSE 8080

# Health check using wget from busybox (already in alpine)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Add memory constraints and GC tuning for Gateway
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Dspring.profiles.active=docker", \
    "-jar", \
    "app.jar"]