# Build stage
FROM maven:3.8.4-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Python & system dependencies
RUN apt-get update && \
    apt-get install -y \
        python3 \
        python3-pip \
        python3-dev \
        gcc \
        g++ \
        libc6-dev \
        wget && \
    # Install Python packages with system override
    pip3 install --break-system-packages --no-cache-dir \
        spacy==3.4.4 \
        scikit-learn==1.1.3 \
        numpy==1.23.5 \
        pandas==1.5.2 && \
    python3 -m spacy download en_core_web_sm && \
    apt-get remove -y gcc g++ python3-dev libc6-dev && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create user and set permissions
RUN groupadd -r spring && \
    useradd -r -g spring spring && \
    mkdir -p /app /app/models && \
    chown -R spring:spring /app

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar
USER spring:spring

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health/liveness || exit 1

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
