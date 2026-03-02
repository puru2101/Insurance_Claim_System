FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/target/user-service-1.0.0.jar user-service.jar
RUN mkdir -p /app/logs /app/uploads && chown -R appuser:appgroup /app
USER appuser
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -q --spider http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "user-service.jar"]
