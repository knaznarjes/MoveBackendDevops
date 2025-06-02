# Build stage
FROM maven:3.8.4-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Run stage - Using standard JRE for ML libraries compatibility
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Python for SpaCy integration and system dependencies
RUN apt-get update && \
    apt-get install -y \
        python3 \
        python3-pip \
        python3-dev \
        gcc \
        g++ \
        libc6-dev \
        wget && \
    # Install Python packages
    pip3 install --no-cache-dir \
        spacy==3.4.4 \
        scikit-learn==1.1.3 \
        numpy==1.23.5 \
        pandas==1.5.2 && \
    # Download SpaCy language model
    python3 -m spacy download en_core_web_sm && \
    # Clean up
    apt-get remove -y gcc g++ python3-dev libc6-dev && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create the spring user and set up permissions
RUN groupadd -r spring && \
    useradd -r -g spring spring && \
    mkdir -p /app /app/models && \
    chown -R spring:spring /app

# Copy the jar file
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to spring user
USER spring:spring

# Expose port for Search Service
EXPOSE 8084

# Health check with longer timeout for ML service
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health/liveness || exit 1

# JVM settings optimized for ML processing
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=85.0", \
    "-XX:+UseG1GC", \
    "-XX:G1HeapRegionSize=16m", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+UseZGC", \
    "-Dspring.profiles.active=docker", \
    "-Dpython.path=/usr/bin/python3", \
    "-jar", \
    "app.jar"]