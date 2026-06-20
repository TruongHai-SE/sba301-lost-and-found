package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.AuthResponse;
import com.sba301.lostandfound.dto.GoogleLoginRequest;
import com.sba301.lostandfound.dto.LoginRequest;
import com.sba301.lostandfound.dto.RefreshTokenResponse;
import com.sba301.lostandfound.dto.RegisterRequest;
import com.sba301.lostandfound.dto.RequestOtpRequest;
import com.sba301.lostandfound.dto.ResetPasswordRequest;
import com.sba301.lostandfound.dto.SetupPasswordRequest;
import com.sba301.lostandfound.dto.SetupPasswordResponse;
import com.sba301.lostandfound.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    RefreshTokenResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void requestForgotPasswordOtp(RequestOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    /**
     * Sets a local password for a Google-only user (password == null).
     * Throws 409 if the user already has a local password.
     * Throws 400 if passwords do not match.
     *
     * @param currentUser the authenticated user extracted from the JWT SecurityContext
     * @param request     the validated setup-password payload
     */
    SetupPasswordResponse setupPassword(User currentUser, SetupPasswordRequest request);
}
