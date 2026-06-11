package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.AuthResponse;
import com.sba301.lostandfound.dto.GoogleLoginRequest;
import com.sba301.lostandfound.dto.LoginRequest;
import com.sba301.lostandfound.dto.RefreshTokenResponse;
import com.sba301.lostandfound.dto.RegisterRequest;
import com.sba301.lostandfound.dto.RequestOtpRequest;
import com.sba301.lostandfound.dto.ResetPasswordRequest;
import com.sba301.lostandfound.security.CookieUtils;
import com.sba301.lostandfound.security.JwtTokenProvider;
import com.sba301.lostandfound.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return buildAuthResponse(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return buildAuthResponse(response, HttpStatus.OK);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return buildAuthResponse(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        RefreshTokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        authService.logout(refreshToken);

        ResponseCookie deleteCookie = CookieUtils.deleteRefreshTokenCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody RequestOtpRequest request) {
        authService.requestForgotPasswordOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(AuthResponse response, HttpStatus status) {
        ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                response.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenExpirationSeconds());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
