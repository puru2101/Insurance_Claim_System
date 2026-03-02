package com.insurance.user.filter;

import com.insurance.user.security.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for user-service.
 *
 * TWO sources of auth info:
 *
 *  1. GATEWAY HEADERS (preferred path):
 *     When requests come through the API Gateway, the gateway already
 *     validated the JWT and injected these headers:
 *       X-Auth-User-Id, X-Auth-Username, X-Auth-Roles
 *     We trust these headers — the gateway is our trusted perimeter.
 *
 *  2. BEARER TOKEN (direct access path):
 *     For direct calls (dev/testing, service-to-service), we validate
 *     the JWT token ourselves.
 *
 *  This dual approach enables both gateway-routed and direct access.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ── Path 1: Trust gateway-injected headers ────────────────────────────
        String userId   = request.getHeader("X-Auth-User-Id");
        String username = request.getHeader("X-Auth-Username");
        String rolesHdr = request.getHeader("X-Auth-Roles");
        String correlationId = request.getHeader("X-Correlation-Id");

        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }

        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            List<SimpleGrantedAuthority> authorities = List.of();
            if (StringUtils.hasText(rolesHdr)) {
                authorities = List.of(rolesHdr.split(",")).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            }

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(userId);   // store userId as details — accessible in controllers
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Auth via gateway headers: user={}, roles={}", username, rolesHdr);

        } else {
            // ── Path 2: Direct Bearer Token validation ─────────────────────────
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String email   = jwtUtil.extractEmail(token);
                Long   tokenUserId = jwtUtil.extractUserId(token);
                List<String> roles = jwtUtil.extractRoles(token);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
                auth.setDetails(tokenUserId != null ? tokenUserId.toString() : null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Auth via JWT token: user={}", email);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
