package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.VerificationAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationAnswerRepository extends JpaRepository<VerificationAnswer, Long> {

    /**
     * Lấy tất cả đáp án chuẩn của 1 câu hỏi xác minh.
     */
    List<VerificationAnswer> findByVerificationId(Long verificationId);

    /**
     * Tìm đáp án chuẩn theo câu hỏi xác minh (thường chỉ có 1 đáp án mỗi câu).
     */
    Optional<VerificationAnswer> findFirstByVerificationId(Long verificationId);
}
