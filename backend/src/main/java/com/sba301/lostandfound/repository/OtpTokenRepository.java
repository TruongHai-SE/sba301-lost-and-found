package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.OtpToken;
import com.sba301.lostandfound.entity.enums.OtpPurpose;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findFirstByUser_MailAndOtpCodeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String mail, String otpCode, OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.used = true OR o.expiresAt < :now")
    int deleteExpiredOrUsed(LocalDateTime now);
}
