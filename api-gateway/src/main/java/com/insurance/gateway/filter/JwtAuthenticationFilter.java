package com.insurance.gateway.filter;

import com.insurance.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication Filter
 *
 * This is the MOST IMPORTANT filter in the gateway.
 * It runs on EVERY request and:
 *
 *   STEP 1: Check if the request is for a public endpoint (login, register)
 *           → If yes: skip validation, pass through
 *
 *   STEP 2: Check if Authorization header exists and starts with "Bearer "
 *           → If no: reject with 401 Unauthorized
 *
 *   STEP 3: Extract and validate the JWT token
 *           → If invalid/expired: reject with 401 Unauthorized
 *
 *   STEP 4: Extract user info from token (userId, username, roles)
 *           and add them as request headers for downstream services
 *           → Downstream services trust these headers (set by gateway only)
 *
 *   STEP 5: Forward the enriched request to the target microservice
 *
 * WHY GlobalFilter?
 * A GlobalFilter applies to ALL routes automatically.
 * You don't need to attach it to each route individually.
 *
 * WHY reactive (Mono)?
 * Spring Cloud Gateway is built on WebFlux (reactive).
 * Mono<Void> = asynchronous response that completes when request is done.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // Run this filter before all others
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Public endpoints that DON'T require a JWT token.
     * These are "whitelisted" — the filter passes them through without auth.
     *
     * Examples:
     *   POST /api/auth/login        → User logging in (doesn't have a token yet!)
     *   POST /api/auth/register     → New user registration
     *   GET  /actuator/health       → Health checks for monitoring
     */
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/actuator/health",
        "/actuator/info",
        "/eureka/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        logger.debug("Gateway received: {} {}", method, path);

        // STEP 1: Is this a public endpoint? Skip auth.
        if (isPublicEndpoint(path)) {
            logger.debug("Public endpoint, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // STEP 2: Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                "Missing or invalid Authorization header. Expected: Bearer <token>");
        }

        // Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // STEP 3: Validate the token
        if (!jwtUtil.isTokenValid(token)) {
            logger.warn("Invalid or expired JWT token for path: {}", path);
            return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                "JWT token is invalid or expired. Please login again.");
        }

        // STEP 4: Extract user details and enrich the request headers
        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            String username = claims.getSubject();
            String userId = jwtUtil.extractUserId(token);
            List<String> roles = jwtUtil.extractRoles(token);

            logger.debug("Authenticated user: {} with roles: {}", username, roles);

            // Add user info as headers — downstream services read these
            // This way, downstream services don't need to parse JWT themselves
            ServerHttpRequest enrichedRequest = request.mutate()
                .header("X-Auth-User-Id", userId != null ? userId : "")
                .header("X-Auth-Username", username)
                .header("X-Auth-Roles", String.join(",", roles))
                .build();

            // STEP 5: Continue the filter chain with enriched request
            return chain.filter(exchange.mutate().request(enrichedRequest).build());

        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage());
            return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                "Error processing authentication token.");
        }
    }

    /**
     * Check if the requested path matches any public endpoint pattern.
     * Uses AntPathMatcher to support wildcards like /eureka/**
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
            .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * Send a JSON error response directly from the gateway.
     * This short-circuits the filter chain — no downstream call is made.
     */
    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Build JSON error body
        String body = String.format(
            "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value()
        );

        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Highest precedence = runs first among all filters
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
