package com.insurance.auth.config;

import com.insurance.auth.filter.JwtAuthenticationFilter;
import com.insurance.auth.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 *
 * Key decisions:
 *  - STATELESS sessions (no cookies/session) — JWT handles state
 *  - CSRF disabled (JWT is CSRF-safe)
 *  - Public endpoints: login, register, refresh, forgot/reset password
 *  - Protected endpoints: /auth/me, /auth/logout, /auth/change-password
 *  - BCrypt for password hashing (strength=12 for production)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no token needed
                .requestMatchers(HttpMethod.POST,
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh-token",
                    "/auth/forgot-password",
                    "/auth/reset-password").permitAll()
                // Health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Add JWT filter before Spring's default auth filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength=12: ~250ms hashing time (good balance security vs performance)
        // Use 10 in dev for faster tests
        return new BCryptPasswordEncoder(12);
    }
}
