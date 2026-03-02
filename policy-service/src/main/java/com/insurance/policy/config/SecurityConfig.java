package com.insurance.policy.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST,   "/policies").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers(HttpMethod.DELETE, "/policies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/policies/admin/**").hasAnyRole("ADMIN", "AGENT")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

@Slf4j
@Component
class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String userId   = req.getHeader("X-Auth-User-Id");
        String username = req.getHeader("X-Auth-Username");
        String rolesHdr = req.getHeader("X-Auth-Roles");

        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            List<SimpleGrantedAuthority> authorities = List.of();
            if (StringUtils.hasText(rolesHdr)) {
                authorities = List.of(rolesHdr.split(",")).stream()
                    .map(String::trim).filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            }
            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            String bearer = req.getHeader("Authorization");
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                try {
                    String token = bearer.substring(7);
                    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                    Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload();
                    String email = claims.getSubject();
                    Object uid   = claims.get("userId");
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles") instanceof List
                        ? (List<String>) claims.get("roles") : List.of();
                    var auth = new UsernamePasswordAuthenticationToken(email, null,
                        roles.stream().map(SimpleGrantedAuthority::new).toList());
                    auth.setDetails(uid != null ? uid.toString() : null);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (JwtException e) {
                    log.warn("Invalid JWT: {}", e.getMessage());
                }
            }
        }
        chain.doFilter(req, res);
    }
}
