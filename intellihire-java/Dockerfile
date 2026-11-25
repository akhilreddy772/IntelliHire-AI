# ── IntelliHire AI — Java/Spring Boot Dockerfile ─────────────────────────────
FROM eclipse-temurin:17-jdk-slim AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Download dependencies and build (skip tests for faster build)
RUN apt-get update && apt-get install -y maven \
    && mvn package -DskipTests -q \
    && rm -rf /root/.m2

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-slim
WORKDIR /app

COPY --from=builder /app/target/intellihire-ai-1.0.0.jar app.jar

RUN mkdir -p uploads data

EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
