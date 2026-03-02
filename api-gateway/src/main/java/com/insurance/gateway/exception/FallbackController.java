package com.insurance.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback Controller - Circuit Breaker Fallback Responses
 *
 * When the Circuit Breaker opens (a service is unhealthy/down),
 * instead of letting requests hang until timeout, the gateway immediately
 * returns a friendly error response from these fallback endpoints.
 *
 * Example flow:
 *   1. Client sends: GET /api/claims/123
 *   2. claim-service is DOWN (circuit is OPEN)
 *   3. Gateway forwards to: /fallback/claim-service (this controller)
 *   4. Client gets immediate 503 with a clear message
 *   (Instead of waiting 30s for a timeout!)
 *
 * This is the "fail fast" principle — return quickly with a clear error
 * rather than making clients wait and wonder.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        logger.warn("User Service circuit breaker activated — service unavailable");
        return buildFallbackResponse("user-service", "User Service is temporarily unavailable.");
    }

    @GetMapping("/claim-service")
    public Mono<ResponseEntity<Map<String, Object>>> claimServiceFallback() {
        logger.warn("Claim Service circuit breaker activated — service unavailable");
        return buildFallbackResponse("claim-service",
            "Claim Service is temporarily unavailable. Your claim data is safe. Please try again shortly.");
    }

    @GetMapping("/policy-service")
    public Mono<ResponseEntity<Map<String, Object>>> policyServiceFallback() {
        logger.warn("Policy Service circuit breaker activated — service unavailable");
        return buildFallbackResponse("policy-service",
            "Policy Service is temporarily unavailable. Please try again shortly.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(
            String service, String message) {

        Map<String, Object> response = Map.of(
            "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
            "error", "Service Unavailable",
            "service", service,
            "message", message,
            "timestamp", LocalDateTime.now().toString(),
            "suggestion", "Please try again in a few minutes. If the issue persists, contact support."
        );

        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response));
    }
}
