package com.insurance.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Gateway Security Configuration
 *
 * IMPORTANT NOTE:
 * The gateway uses Spring WebFlux (reactive), so it uses:
 *   - ServerHttpSecurity (not HttpSecurity)
 *   - @EnableWebFluxSecurity (not @EnableWebSecurity)
 *   - SecurityWebFilterChain (not SecurityFilterChain)
 *
 * WHY disable Spring Security's built-in auth here?
 * Our JWT filter (JwtAuthenticationFilter) handles all authentication logic.
 * Spring Security's default would conflict with it, so we disable it
 * and let our custom filter be the single source of auth truth.
 *
 * CORS (Cross-Origin Resource Sharing):
 * Without CORS config, browsers would block requests from your frontend
 * (e.g., React app on port 3000) to the gateway (port 8080).
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF — APIs use tokens (JWT), not cookies; CSRF not applicable
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // Enable CORS with our custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable Spring Security's built-in form login and HTTP Basic
            // Our JWT filter handles authentication
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

            // Allow all requests — our JWT filter handles auth (not Spring Security's default)
            // The JWT filter ALREADY blocks unauthorized requests before they reach here
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )

            .build();
    }

    /**
     * CORS Configuration
     *
     * Allows frontend applications (React, Angular, mobile) to call the gateway.
     *
     * In production:
     *   - Replace allowedOrigins with your specific frontend domain(s)
     *   - Don't use "*" in production — it's a security risk
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow specific origins (in dev: all, in prod: your frontend URL)
        config.setAllowedOriginPatterns(List.of("*"));

        // Allow standard HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allow these headers in requests
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "X-Correlation-Id"
        ));

        // Expose these headers in responses (frontend can read them)
        config.setExposedHeaders(List.of(
            "X-Correlation-Id",
            "X-Total-Count"
        ));

        // Allow cookies/auth headers in cross-origin requests
        config.setAllowCredentials(true);

        // Cache preflight response for 3600 seconds (1 hour)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
