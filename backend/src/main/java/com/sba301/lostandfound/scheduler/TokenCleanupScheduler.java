package com.sba301.lostandfound.scheduler;

import com.sba301.lostandfound.repository.OtpTokenRepository;
import com.sba301.lostandfound.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;

    /**
     * Periodic cleanup of expired and revoked tokens.
     * Configured to run after a short initial delay on startup, and then at a regular interval.
     */
    @Scheduled(initialDelayString = "${app.cleanup.initial-delay:10000}", fixedRateString = "${app.cleanup.interval:1800000}")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting periodic cleanup of expired/revoked tokens...");
        LocalDateTime now = LocalDateTime.now();
        
        try {
            int deletedRefreshTokens = refreshTokenRepository.deleteExpiredOrRevoked(now);
            int deletedOtpTokens = otpTokenRepository.deleteExpiredOrUsed(now);
            
            log.info("Token cleanup completed successfully. Deleted {} refresh token(s) and {} OTP token(s).",
                    deletedRefreshTokens, deletedOtpTokens);
        } catch (Exception e) {
            log.error("Failed to run periodic token cleanup", e);
        }
    }
}
