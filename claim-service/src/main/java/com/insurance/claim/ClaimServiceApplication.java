package com.insurance.claim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Claim Service — Core Business Service of the Insurance Claim System
 *
 * Responsibilities:
 *  - Full insurance claim lifecycle: SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED / SETTLED
 *  - Claim document attachments
 *  - Claim notes / comments (agent ↔ customer)
 *  - Claim statistics & reporting
 *  - Event-driven: publishes Kafka events on every status change
 *  - Inter-service: calls user-service via WebClient to validate users
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class ClaimServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(ClaimServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ClaimServiceApplication.class, args);
        logger.info("==============================================");
        logger.info("  Claim Service - RUNNING on port 8083");
        logger.info("==============================================");
    }
}
