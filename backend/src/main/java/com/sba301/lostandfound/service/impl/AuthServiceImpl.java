package com.sba301.lostandfound.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sba301.lostandfound.dto.AuthResponse;
import com.sba301.lostandfound.dto.GoogleLoginRequest;
import com.sba301.lostandfound.dto.LoginRequest;
import com.sba301.lostandfound.dto.RefreshTokenResponse;
import com.sba301.lostandfound.dto.RegisterRequest;
import com.sba301.lostandfound.dto.RequestOtpRequest;
import com.sba301.lostandfound.dto.ResetPasswordRequest;
import com.sba301.lostandfound.dto.SetupPasswordRequest;
import com.sba301.lostandfound.dto.SetupPasswordResponse;
import com.sba301.lostandfound.entity.OtpToken;
import com.sba301.lostandfound.entity.RefreshToken;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.OtpPurpose;
import com.sba301.lostandfound.entity.enums.UserType;
import com.sba301.lostandfound.repository.OtpTokenRepository;
import com.sba301.lostandfound.repository.RefreshTokenRepository;
import com.sba301.lostandfound.repository.UserRepository;
import com.sba301.lostandfound.security.CustomUserDetails;
import com.sba301.lostandfound.security.JwtTokenProvider;
import com.sba301.lostandfound.service.AuthService;
import com.sba301.lostandfound.service.EmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int OTP_EXPIRY_MINUTES = 5;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            OtpTokenRepository otpTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailService emailService,
            @Value("${google.client-id}") String googleClientId) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;

        try {
            this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Google token verifier", e);
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByMail(request.getMail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .mail(request.getMail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .type(UserType.USER)
                .build();
        userRepository.save(user);

        return buildAuthResponseWithRefreshToken(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByMail(request.getMail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getPassword() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "This account uses Google login. Please use 'Forgot Password' to set a password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return buildAuthResponseWithRefreshToken(user);
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(request.getIdToken());
        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }

        if (idToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByMail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name != null ? name : email)
                            .mail(email)
                            .type(UserType.USER)
                            .build();
                    return userRepository.save(newUser);
                });

        return buildAuthResponseWithRefreshToken(user);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(String refreshTokenStr) {
        if (refreshTokenStr == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }

        if (!jwtTokenProvider.validateToken(refreshTokenStr) || !"REFRESH".equals(jwtTokenProvider.getTokenTypeFromToken(refreshTokenStr))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Refresh token has been revoked"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        CustomUserDetails userDetails = new CustomUserDetails(storedToken.getUser());
        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null) {
            refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr)
                    .ifPresent(RefreshToken::revoke);
        }
    }

    @Override
    @Transactional
    public void requestForgotPasswordOtp(RequestOtpRequest request) {
        User user = userRepository.findByMail(request.getMail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String otpCode = generateOtp();
        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .otpCode(otpCode)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpTokenRepository.save(otpToken);

        emailService.sendOtpEmail(user.getMail(), otpCode, "Password Reset");
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByMail(request.getMail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        verifyOtp(request.getMail(), request.getOtp(), OtpPurpose.FORGOT_PASSWORD);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens after password reset
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Override
    @Transactional
    public SetupPasswordResponse setupPassword(User currentUser, SetupPasswordRequest request) {
        if (currentUser.getPassword() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Password already set. Use 'Change Password' instead.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Passwords do not match.");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        log.info("User id={} set up a local password successfully.", currentUser.getId());

        return SetupPasswordResponse.builder()
                .hasPassword(true)
                .message("Password set successfully. You can now log in with email and password.")
                .build();
    }

    private void verifyOtp(String mail, String otpCode, OtpPurpose purpose) {
        OtpToken otpToken = otpTokenRepository
                .findFirstByUser_MailAndOtpCodeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        mail, otpCode, purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP code"));

        if (otpToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }

        otpToken.markUsed();
        otpTokenRepository.save(otpToken);
    }

    private AuthResponse buildAuthResponseWithRefreshToken(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()))
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .name(user.getName())
                .mail(user.getMail())
                .userType(user.getType().name())
                .build();
    }

    private String generateOtp() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }
}
