package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.Verification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    /**
     * Lấy tất cả câu hỏi xác minh của 1 post, sắp xếp theo thứ tự.
     */
    List<Verification> findByPostIdOrderByQuestionIndexAsc(Long postId);
}
