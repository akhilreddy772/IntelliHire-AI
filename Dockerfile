# ── IntelliHire AI — Root Dockerfile (Java/Spring Boot for Render.com) ────────
# Multi-stage build: Maven build → JRE runtime
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache for deps)
COPY intellihire-java/pom.xml ./pom.xml
COPY intellihire-java/src ./src

# Install Maven and build the JAR (skip tests for faster deploy)
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && mvn package -DskipTests -q \
    && rm -rf /root/.m2 /var/lib/apt/lists/*

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/intellihire-ai-1.0.0.jar app.jar

# Copy static data (jobs.json, etc.)
COPY intellihire-java/src/main/resources/data ./data

RUN mkdir -p uploads data

EXPOSE 8080

# Render injects $PORT — Spring Boot reads server.port from env
ENV JAVA_OPTS="-Xmx450m -Xms200m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
