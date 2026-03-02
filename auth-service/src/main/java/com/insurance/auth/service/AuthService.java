package com.insurance.auth.service;

import com.insurance.auth.dto.request.*;
import com.insurance.auth.dto.response.AuthResponse;
import com.insurance.auth.dto.response.UserResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    UserResponse register(RegisterRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String token);

    UserResponse getCurrentUser(String email);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String email, ChangePasswordRequest request);
}
