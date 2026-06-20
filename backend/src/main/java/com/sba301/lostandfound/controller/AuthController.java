package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.AuthResponse;
import com.sba301.lostandfound.dto.GoogleLoginRequest;
import com.sba301.lostandfound.dto.LoginRequest;
import com.sba301.lostandfound.dto.RefreshTokenResponse;
import com.sba301.lostandfound.dto.RegisterRequest;
import com.sba301.lostandfound.dto.RequestOtpRequest;
import com.sba301.lostandfound.dto.ResetPasswordRequest;
import com.sba301.lostandfound.dto.SetupPasswordRequest;
import com.sba301.lostandfound.dto.SetupPasswordResponse;
import com.sba301.lostandfound.dto.UserResponse;
import com.sba301.lostandfound.security.CookieUtils;
import com.sba301.lostandfound.security.CustomUserDetails;
import com.sba301.lostandfound.security.JwtTokenProvider;
import com.sba301.lostandfound.service.AuthService;
import com.sba301.lostandfound.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, token management and account self-service")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final boolean secureCookie;

    public AuthController(AuthService authService,
                          UserService userService,
                          JwtTokenProvider jwtTokenProvider,
                          @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.authService = authService;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return buildAuthResponse(response, HttpStatus.CREATED, "User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return buildAuthResponse(response, HttpStatus.OK, "Login successful");
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return buildAuthResponse(response, HttpStatus.OK, "Login successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        RefreshTokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        authService.logout(refreshToken);

        ResponseCookie deleteCookie = CookieUtils.deleteRefreshTokenCookie(secureCookie);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody RequestOtpRequest request) {
        authService.requestForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @PostMapping("/password/setup")
    @Operation(
        summary = "Set up local password",
        description = "Allows a Google-only account (no local password) to create a local password. "
                    + "Requires a valid Access Token. Returns 409 if a password is already set."
    )
    public ResponseEntity<ApiResponse<SetupPasswordResponse>> setupPassword(
            @AuthenticationPrincipal CustomUserDetails currentUserDetails,
            @Valid @RequestBody SetupPasswordRequest request) {
        SetupPasswordResponse response = authService.setupPassword(currentUserDetails.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Password set up successfully"));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile of the currently authenticated user, including hasPassword flag "
                    + "which indicates whether the account has a local password set."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        UserResponse response = userService.getUserById(currentUserDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved successfully"));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> buildAuthResponse(AuthResponse response, HttpStatus status, String message) {
        ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                response.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenExpirationSeconds(),
                secureCookie);
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(status.value(), response, message);
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(apiResponse);
    }
}
