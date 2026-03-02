package com.insurance.eureka.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Eureka Server Dashboard.
 *
 * WHY secure the registry?
 * - Without security, anyone could register fake services or read your registry
 * - In production, the registry contains internal service URLs — sensitive info!
 * - We use HTTP Basic Auth here (simple username/password) since this is server-to-server
 *
 * NOTE: In a real production setup, you'd use mutual TLS or OAuth2 client credentials.
 * HTTP Basic is shown here for clarity and ease of learning.
 */
@Configuration
@EnableWebSecurity
public class EurekaSecurityConfig {

    @Value("${eureka.security.username:eureka-admin}")
    private String username;

    @Value("${eureka.security.password:eureka-secret}")
    private String password;

    /**
     * Defines which requests are allowed and which require authentication.
     * Eureka clients need to POST to /eureka/** to register, so we allow that with credentials.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for Eureka clients (they're not browsers, so no CSRF risk)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**")
            )
            // All requests require login (HTTP Basic Auth)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            // Enable HTTP Basic Authentication for REST clients (microservices)
            .httpBasic(httpBasic -> {})
            // Enable form login for the Eureka web dashboard (browser access)
            .formLogin(form -> form.defaultSuccessUrl("/", true));

        return http.build();
    }

    /**
     * In-memory user store for Eureka authentication.
     * In production, consider externalizing credentials to a secrets manager (AWS Secrets Manager, Vault).
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        UserDetails eurekaUser = User.builder()
            .username(username)
            .password(passwordEncoder().encode(password))
            .roles("EUREKA_CLIENT", "ADMIN")
            .build();

        return new InMemoryUserDetailsManager(eurekaUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
