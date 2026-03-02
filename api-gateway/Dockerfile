# ============================================================
#  Dockerfile - API Gateway
# ============================================================

FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/api-gateway-1.0.0.jar api-gateway.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

# Gateway runs on port 8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Xms256m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "api-gateway.jar"]
