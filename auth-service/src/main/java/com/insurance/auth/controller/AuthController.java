package com.insurance.auth.controller;

import com.insurance.auth.dto.request.*;
import com.insurance.auth.dto.response.ApiResponse;
import com.insurance.auth.dto.response.AuthResponse;
import com.insurance.auth.dto.response.UserResponse;
import com.insurance.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController
 *
 * REST API for all authentication operations.
 * Base path: /auth  (gateway strips /api prefix, so this becomes /api/auth/*)
 *
 * PUBLIC endpoints (no JWT needed):
 *   POST /auth/login
 *   POST /auth/register
 *   POST /auth/refresh-token
 *   POST /auth/forgot-password
 *   POST /auth/reset-password
 *
 * PROTECTED endpoints (JWT required):
 *   GET  /auth/me
 *   POST /auth/logout
 *   POST /auth/change-password
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── Public Endpoints ────────────────────────────────────────────────────

    /**
     * POST /auth/login
     * Authenticate user and return JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse authResponse = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * POST /auth/register
     * Register a new user (default role: CUSTOMER).
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = authService.register(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Registration successful. Welcome!", user));
    }

    /**
     * POST /auth/refresh-token
     * Exchange a valid refresh token for a new access token.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    /**
     * POST /auth/forgot-password
     * Request a password reset email.
     * Always returns 200 to prevent email enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
            "If that email is registered, a reset link has been sent."));
    }

    /**
     * POST /auth/reset-password
     * Reset password using the token from email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. Please login."));
    }

    // ─── Protected Endpoints (require valid JWT) ─────────────────────────────

    /**
     * GET /auth/me
     * Get current authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User profile", user));
    }

    /**
     * POST /auth/logout
     * Invalidate the refresh token (access token expires naturally).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request) {

        String token = extractTokenFromRequest(request);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * POST /auth/change-password
     * Change password for authenticated user.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(
            "Password changed successfully. Please login again."));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
