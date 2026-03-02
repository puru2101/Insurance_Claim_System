package com.insurance.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class PolicyServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(PolicyServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PolicyServiceApplication.class, args);
        logger.info("==============================================");
        logger.info("  Policy Service - RUNNING on port 8084");
        logger.info("==============================================");
    }
}
