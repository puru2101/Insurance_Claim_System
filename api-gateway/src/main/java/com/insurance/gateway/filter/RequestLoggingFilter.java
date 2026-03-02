package com.insurance.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Request Logging Filter
 *
 * Logs every request and response that passes through the gateway.
 * Adds a unique "Correlation ID" to each request for distributed tracing.
 *
 * CORRELATION ID: A unique ID (UUID) assigned to each request at the gateway.
 * This ID travels with the request to all downstream services.
 * When something goes wrong, you can search logs by correlation ID to
 * trace the entire journey of a request across all services.
 *
 * Example log output:
 *   [CORR-abc123] → POST /api/claims [user: john@example.com] started
 *   [CORR-abc123] → POST /api/claims completed in 245ms with status 201
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Generate or reuse a correlation ID
        String correlationId = exchange.getRequest().getHeaders()
            .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        final String finalCorrelationId = correlationId;

        // Add correlation ID to MDC so it appears in all log messages
        MDC.put("correlationId", finalCorrelationId);

        // Record start time for response time calculation
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_ATTRIBUTE, startTime);

        // Log the incoming request
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String clientIp = getClientIp(exchange);
        String username = exchange.getRequest().getHeaders().getFirst("X-Auth-Username");

        logger.info("[{}] → {} {} from IP: {} | User: {}",
            finalCorrelationId, method, path, clientIp,
            username != null ? username : "anonymous");

        // Add correlation ID to request forwarded to downstream services
        var enrichedRequest = exchange.getRequest().mutate()
            .header(CORRELATION_ID_HEADER, finalCorrelationId)
            .build();

        var enrichedExchange = exchange.mutate().request(enrichedRequest).build();

        return chain.filter(enrichedExchange)
            .then(Mono.fromRunnable(() -> {
                // Log the response after completion
                long duration = System.currentTimeMillis() -
                    (Long) exchange.getAttributes().get(START_TIME_ATTRIBUTE);
                int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;

                if (statusCode >= 400) {
                    logger.warn("[{}] ← {} {} completed in {}ms with status {}",
                        finalCorrelationId, method, path, duration, statusCode);
                } else {
                    logger.info("[{}] ← {} {} completed in {}ms with status {}",
                        finalCorrelationId, method, path, duration, statusCode);
                }

                MDC.clear();
            }));
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        // Run AFTER JWT filter (which has HIGHEST_PRECEDENCE)
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
