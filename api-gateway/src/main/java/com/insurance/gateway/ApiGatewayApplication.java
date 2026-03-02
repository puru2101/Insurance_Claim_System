package com.insurance.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway - The Single Entry Point for Insurance Claim System
 *
 * WHAT IT DOES:
 *   - Acts as the "front door" for ALL client requests (mobile app, web app, Postman)
 *   - Routes requests to the correct microservice (e.g., /api/claims → claim-service)
 *   - Validates JWT tokens BEFORE forwarding requests (no auth-service round trip per request)
 *   - Applies Cross-Cutting Concerns:
 *       * Authentication & Authorization (JWT validation)
 *       * Rate Limiting (prevent abuse)
 *       * Circuit Breaking (fallback when a service is down)
 *       * Request/Response Logging
 *       * CORS handling
 *
 * WHY USE A GATEWAY?
 *   Without a gateway, every client would need to know:
 *     - The URL of every service
 *     - How to authenticate with each service
 *     - How to handle service failures
 *   The gateway centralizes all of this!
 *
 * TECHNOLOGY: Spring Cloud Gateway uses reactive WebFlux (non-blocking I/O),
 * which means it handles thousands of concurrent connections efficiently.
 */
@SpringBootApplication
@EnableDiscoveryClient  // Register this gateway in Eureka
public class ApiGatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        logger.info("=======================================================");
        logger.info("  Insurance Claim System - API Gateway");
        logger.info("  Status: RUNNING");
        logger.info("  Port: 8080");
        logger.info("  All requests enter through this gateway");
        logger.info("=======================================================");
    }
}
