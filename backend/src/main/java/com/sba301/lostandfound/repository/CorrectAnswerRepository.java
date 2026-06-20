package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.CorrectAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectAnswerRepository extends JpaRepository<CorrectAnswer, Long> {

    /**
     * Lấy tất cả đáp án đúng của 1 verification.
     */
    List<CorrectAnswer> findByVerificationId(Long verificationId);

    /**
     * Tìm đáp án đúng theo verification (thường chỉ có 1).
     */
    Optional<CorrectAnswer> findFirstByVerificationId(Long verificationId);
}
