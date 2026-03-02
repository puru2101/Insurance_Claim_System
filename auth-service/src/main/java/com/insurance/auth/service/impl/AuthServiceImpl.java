package com.insurance.auth.service.impl;

import com.insurance.auth.dto.request.*;
import com.insurance.auth.dto.response.AuthResponse;
import com.insurance.auth.dto.response.UserResponse;
import com.insurance.auth.entity.AuditLog;
import com.insurance.auth.entity.AuditLog.AuditAction;
import com.insurance.auth.entity.Role;
import com.insurance.auth.entity.User;
import com.insurance.auth.exception.*;
import com.insurance.auth.mapper.UserMapper;
import com.insurance.auth.repository.AuditLogRepository;
import com.insurance.auth.repository.RoleRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.auth.security.JwtTokenProvider;
import com.insurance.auth.service.AuthService;
import com.insurance.auth.service.KafkaPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final KafkaPublisherService kafkaPublisher;

    // Password reset token validity: 1 hour
    private static final long PASSWORD_RESET_TOKEN_VALIDITY_HOURS = 1;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        try {
            // Spring Security authenticates (calls UserDetailsServiceImpl internally)
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );

            // Auth successful — load full user entity
            User user = userRepository
                .findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.getIsActive()) {
                throw new AccountDisabledException("Your account has been deactivated. Contact support.");
            }

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // Persist refresh token and update last login
            userRepository.updateRefreshToken(user.getId(), refreshToken);
            userRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());

            // Audit trail
            saveAuditLog(user.getId(), user.getEmail(), AuditAction.LOGIN_SUCCESS,
                ipAddress, userAgent, "Login successful", true);

            // Publish Kafka event (async — non-blocking)
            kafkaPublisher.publishUserLoggedIn(user.getId(), user.getEmail());

            log.info("Login successful for user: {}", user.getEmail());

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                    .map(r -> r.getRoleName().name())
                    .toList())
                .build();

        } catch (BadCredentialsException e) {
            saveAuditLog(null, request.getUsernameOrEmail(), AuditAction.LOGIN_FAILURE,
                ipAddress, userAgent, "Invalid credentials", false);
            throw new InvalidCredentialsException("Invalid username/email or password");

        } catch (LockedException e) {
            throw new AccountDisabledException("Account is locked. Contact support.");

        } catch (DisabledException e) {
            throw new AccountDisabledException("Account is disabled. Contact support.");
        }
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken: " + request.getUsername());
        }

        // Assign default CUSTOMER role
        Role customerRole = roleRepository.findByRoleName(Role.RoleName.ROLE_CUSTOMER)
            .orElseThrow(() -> new ResourceNotFoundException("Default role not found. Run DB seed."));

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .isActive(true)
            .isEmailVerified(false)
            .roles(Set.of(customerRole))
            .build();

        User savedUser = userRepository.save(user);

        // Audit + Kafka event
        saveAuditLog(savedUser.getId(), savedUser.getEmail(), AuditAction.REGISTER,
            null, null, "New user registered", true);
        kafkaPublisher.publishUserRegistered(savedUser.getId(), savedUser.getEmail(),
            savedUser.getFirstName());

        log.info("User registered successfully: {}", savedUser.getEmail());
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Refresh token is invalid or expired. Please login again.");
        }

        User user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not recognized. Please login again."));

        // Generate new access token (refresh token stays the same)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        log.debug("Access token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)   // return same refresh token
            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(user.getRoles().stream().map(r -> r.getRoleName().name()).toList())
            .build();
    }

    @Override
    @Transactional
    public void logout(String token) {
        String email = jwtTokenProvider.extractEmail(token);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Invalidate refresh token in DB — can't get new access tokens anymore
        userRepository.updateRefreshToken(user.getId(), null);

        saveAuditLog(user.getId(), user.getEmail(), AuditAction.LOGOUT,
            null, null, "User logged out", true);

        log.info("User logged out: {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration attacks
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(
                LocalDateTime.now().plusHours(PASSWORD_RESET_TOKEN_VALIDITY_HOURS));
            userRepository.save(user);

            // Publish Kafka event → notification-service will send the email
            kafkaPublisher.publishPasswordResetRequested(user.getEmail(), resetToken);

            saveAuditLog(user.getId(), user.getEmail(), AuditAction.PASSWORD_RESET_REQUEST,
                null, null, "Password reset token generated", true);

            log.info("Password reset token sent for: {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (user.getPasswordResetTokenExpiry() == null ||
            LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new InvalidTokenException("Password reset token has expired. Request a new one.");
        }

        userRepository.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));

        saveAuditLog(user.getId(), user.getEmail(), AuditAction.PASSWORD_RESET_SUCCESS,
            null, null, "Password reset successful", true);

        kafkaPublisher.publishPasswordChanged(user.getEmail());
        log.info("Password reset successful for: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        userRepository.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));

        // Invalidate refresh token — force re-login after password change
        userRepository.updateRefreshToken(user.getId(), null);

        saveAuditLog(user.getId(), user.getEmail(), AuditAction.PASSWORD_CHANGE,
            null, null, "Password changed", true);

        kafkaPublisher.publishPasswordChanged(user.getEmail());
        log.info("Password changed for: {}", email);
    }

    // ─── Helpers ────────────────────────────────────────────

    private void saveAuditLog(Long userId, String username, AuditAction action,
                               String ipAddress, String userAgent,
                               String details, boolean success) {
        try {
            auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .success(success)
                .build());
        } catch (Exception e) {
            // Audit failure must never break the main flow
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
}
