package com.insurance.auth.security;

import com.insurance.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtTokenProvider
 *
 * Responsible for:
 *  1. Generating access tokens (short-lived: 15min–24h)
 *  2. Generating refresh tokens (long-lived: 7–30 days)
 *  3. Validating tokens
 *  4. Extracting claims (userId, username, roles) from tokens
 *
 * TOKEN STRUCTURE (JWT has 3 parts, base64-encoded, separated by dots):
 *   Header.Payload.Signature
 *
 *   Header:  { "alg": "HS256", "typ": "JWT" }
 *   Payload: { "sub": "user@email.com", "userId": 42, "roles": ["ROLE_CUSTOMER"], "exp": 1234567890 }
 *   Signature: HMAC-SHA256(header + "." + payload, secretKey)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate an Access Token for authenticated user.
     * This is the short-lived token sent with every API request.
     *
     * Claims embedded:
     *   - sub (subject): user's email
     *   - userId: database ID
     *   - username: login username
     *   - roles: list of role strings
     *   - iat: issued at
     *   - exp: expiry
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        List<String> roles = user.getRoles().stream()
            .map(role -> role.getRoleName().name())
            .collect(Collectors.toList());

        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("username", user.getUsername())
            .claim("firstName", user.getFirstName())
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Generate a Refresh Token.
     * Long-lived, used only to get a new access token.
     * Stored in the database so we can invalidate it on logout.
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("tokenType", "REFRESH")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Validate a JWT token.
     * Returns true only if:
     *   - Signature is valid (wasn't tampered with)
     *   - Token hasn't expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
