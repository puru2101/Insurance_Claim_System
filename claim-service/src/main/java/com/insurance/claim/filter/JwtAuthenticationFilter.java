package com.insurance.claim.filter;

import com.insurance.claim.security.JwtUtil;
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
 * JWT filter — dual path (gateway headers OR direct bearer token).
 * Identical pattern to user-service — shared approach across all downstream services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String userId   = req.getHeader("X-Auth-User-Id");
        String username = req.getHeader("X-Auth-Username");
        String rolesHdr = req.getHeader("X-Auth-Roles");
        String corrId   = req.getHeader("X-Correlation-Id");

        if (StringUtils.hasText(corrId)) MDC.put("correlationId", corrId);

        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            // Gateway path — trust injected headers
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
            // Direct token path
            String token = extractToken(req);
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                Long tokenUserId = jwtUtil.extractUserId(token);
                List<SimpleGrantedAuthority> authorities = jwtUtil.extractRoles(token).stream()
                    .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                auth.setDetails(tokenUserId != null ? tokenUserId.toString() : null);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }

    private String extractToken(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer "))
            ? bearer.substring(7) : null;
    }
}
