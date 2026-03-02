package com.insurance.eureka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Registry Server
 *
 * This is the central service registry for the Insurance Claim System.
 * All microservices register themselves here, enabling:
 *   - Service Discovery: Services find each other by name, not hardcoded URLs
 *   - Load Balancing: Multiple instances of a service are auto-balanced
 *   - Health Monitoring: Eureka tracks which services are up/down
 *
 * Think of it like a "phone directory" for microservices.
 * Instead of each service knowing the exact address of others,
 * they all look up the registry.
 */
@SpringBootApplication
@EnableEurekaServer  // This annotation is all it takes to make this a registry!
public class EurekaServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(EurekaServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        logger.info("=======================================================");
        logger.info("  Insurance Claim System - Eureka Service Registry");
        logger.info("  Status: RUNNING");
        logger.info("  Dashboard: http://localhost:8761");
        logger.info("=======================================================");
    }
}
