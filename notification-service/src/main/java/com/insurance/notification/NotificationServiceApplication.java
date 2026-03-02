package com.insurance.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service
 *
 * Pure event-driven — consumes Kafka events from auth-service and claim-service,
 * then sends emails / SMS / in-app notifications.
 *
 * No REST API (no inbound HTTP) — it only reads from Kafka.
 * This means it has ZERO coupling to other services at runtime.
 *
 * Kafka topics consumed:
 *   auth.user.registered           → Welcome email
 *   auth.user.passwordResetRequested→ Password reset email
 *   auth.user.passwordChanged       → Password change confirmation
 *   claim.events                   → Claim lifecycle emails
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class NotificationServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        logger.info("==============================================");
        logger.info("  Notification Service - RUNNING on port 8085");
        logger.info("  Consuming Kafka events...");
        logger.info("==============================================");
    }
}
