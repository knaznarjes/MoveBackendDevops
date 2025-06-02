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

# Install ImageMagick for image processing
RUN apk add --no-cache imagemagick

# Create the spring user and set up permissions
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /app /app/uploads /app/temp && \
    chown -R spring:spring /app

# Copy the jar file
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to spring user
USER spring:spring

# Expose port for Content Service
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health/liveness || exit 1

# JVM settings optimized for Media handling (higher memory)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=80.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxDirectMemorySize=256m", \
    "-Dspring.profiles.active=docker", \
    "-Dfile.encoding=UTF-8", \
    "-Djava.awt.headless=true", \
    "-jar", \
    "app.jar"]