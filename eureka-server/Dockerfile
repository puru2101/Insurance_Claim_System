# ============================================================
#  Dockerfile - Eureka Server
#  Multi-stage build: smaller final image
# ============================================================

# Stage 1: Build the application
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml first — Docker caches this layer
# Only re-downloads dependencies when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Create a minimal runtime image
# Eclipse Temurin 17 JRE (smaller than JDK)
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security (never run as root in production!)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built jar from builder stage
COPY --from=builder /app/target/eureka-server-1.0.0.jar eureka-server.jar

# Create logs directory with correct permissions
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose Eureka port
EXPOSE 8761

# Health check — Docker will restart container if unhealthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8761/actuator/health || exit 1

# Run the application
# -Xmx256m: limit heap memory (right-size for a registry server)
ENTRYPOINT ["java", \
    "-Xmx256m", \
    "-Xms128m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "eureka-server.jar"]
