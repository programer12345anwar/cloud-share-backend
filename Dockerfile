# Multi-stage Dockerfile for Spring Boot 3.5.7 + Java 21
# Production-grade setup for Render, Railway, Heroku, etc.

# ===== STAGE 1: BUILD =====
# Build the application in a Maven container
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ===== STAGE 2: RUNTIME =====
# Production runtime with minimal image size
FROM eclipse-temurin:21-jre-jammy

# Install curl for health checks (minimal image)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN useradd -m -u 1000 appuser

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/cloudshareapi-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port (default 8080, can be overridden)
EXPOSE ${SERVER_PORT:-8080}

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8080}/v3/api-docs || exit 1

# Environment variables (can be overridden at runtime)
ENV JAVA_TOOL_OPTIONS="-Xmx512m -Xms256m -XX:+UseG1GC"

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
