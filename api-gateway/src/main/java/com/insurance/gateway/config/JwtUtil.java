package com.insurance.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT Utility - Handles token validation at the Gateway level
 *
 * WHY validate at the gateway?
 * - Prevents unauthorized requests from reaching downstream services
 * - Downstream services trust the gateway and don't need to re-validate
 * - Single point of truth for JWT configuration
 *
 * NOTE: The gateway ONLY validates tokens (reads them).
 * Token CREATION happens in auth-service.
 * Both use the SAME secret key — this is critical!
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /**
     * Build the signing key from the shared secret.
     * HMAC-SHA256 is used — symmetric key, same for signing and verifying.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract all claims from a JWT token.
     * Claims = the payload data (userId, roles, expiry, etc.)
     *
     * @throws ExpiredJwtException if token is expired
     * @throws MalformedJwtException if token format is invalid
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Validate a JWT token.
     * Returns true only if:
     *   1. Signature is valid (token wasn't tampered with)
     *   2. Token hasn't expired
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Check expiration explicitly
            boolean isExpired = claims.getExpiration().before(new Date());
            if (isExpired) {
                logger.warn("JWT token is expired");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token format: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract the username (subject) from a JWT token.
     * The subject is typically the user's email or username.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract the user's roles from the JWT token.
     * Roles are stored as a list in the "roles" claim.
     * Example: ["ROLE_ADMIN", "ROLE_AGENT"]
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    /**
     * Extract the user ID from the JWT token.
     * Used to pass the authenticated user's ID to downstream services.
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : null;
    }
}
