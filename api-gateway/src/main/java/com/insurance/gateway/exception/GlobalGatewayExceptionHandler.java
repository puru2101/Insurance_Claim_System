package com.insurance.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Global Exception Handler for the API Gateway
 *
 * Catches exceptions that occur during routing/filtering and returns
 * a consistent, clean JSON error response instead of Spring's default
 * HTML error page or raw stack trace.
 *
 * Handles common scenarios:
 *   - Service not found (503): A downstream service is down
 *   - Route not found (404): No matching route for the request
 *   - Timeout (504): Downstream service took too long
 *   - General errors (500): Unexpected errors
 */
@Component
@Order(-1)  // Run before Spring Boot's default error handler
public class GlobalGatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalGatewayExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status;
        String message;

        if (ex instanceof NotFoundException) {
            // Service registered in Eureka but no healthy instances available
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "The requested service is currently unavailable. Please try again later.";
            logger.error("Service unavailable for path: {} | Error: {}", path, ex.getMessage());

        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : "Request processing error";
            logger.warn("Response status exception for path: {} | Status: {} | Reason: {}",
                path, status, message);

        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "The upstream service did not respond in time. Please try again.";
            logger.error("Timeout for path: {}", path);

        } else if (ex instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Unable to connect to the service. Please try again later.";
            logger.error("Connection refused for path: {} | Error: {}", path, ex.getMessage());

        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred. Please contact support.";
            logger.error("Unexpected gateway error for path: {} | Error: {}", path, ex.getMessage(), ex);
        }

        // Build JSON error response
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String responseBody = buildErrorJson(status, message, path, correlationId);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        var buffer = exchange.getResponse().bufferFactory()
            .wrap(responseBody.getBytes());

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String buildErrorJson(HttpStatus status, String message, String path, String correlationId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return String.format(
            "{" +
            "\"timestamp\":\"%s\"," +
            "\"status\":%d," +
            "\"error\":\"%s\"," +
            "\"message\":\"%s\"," +
            "\"path\":\"%s\"," +
            "\"correlationId\":\"%s\"" +
            "}",
            timestamp,
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            correlationId != null ? correlationId : "N/A"
        );
    }
}
