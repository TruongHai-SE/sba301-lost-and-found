package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.ClaimAnswerRequest;
import com.sba301.lostandfound.dto.ClaimResponse;
import com.sba301.lostandfound.dto.VerificationQuestion;
import com.sba301.lostandfound.dto.VerificationQuestionsResponse;
import java.util.List;

/**
 * Service xử lý câu hỏi xác minh do AI sinh ra.
 *
 * <p>Flow mới (Approach 2: AI hỏi - AI tự trả lời):
 *  1. AI sinh câu hỏi + tự đưa đáp án chuẩn → lưu vào verifications + verification_answers.
 *  2. Người mất đồ xem được câu hỏi kèm ảnh mờ, gửi câu trả lời qua claim.
 *  3. Server so sánh với verification_answers, nếu đủ score → trả FullPostDetails (ảnh rõ + liên hệ).
 *
 * <p>Hợp đồng:
 *  - getQuestionsForPost: lấy câu hỏi (cho chủ post xem hoặc claimer).
 *  - getQuestionDtos: helper cho mapper.
 *  - claim: xử lý claim flow, trả về ClaimResponse (approved/rejected + details nếu OK).
 *  - scoreAnswer: helper so sánh 1 câu trả lời.
 */
public interface VerificationService {

    /**
     * Lấy danh sách câu hỏi xác minh của 1 post.
     * Trả về response với status: PENDING (có câu hỏi) hoặc NOT_GENERATED (AI chưa sinh).
     */
    VerificationQuestionsResponse getQuestionsForPost(Long postId);

    /**
     * Lấy raw list câu hỏi (cho internal use, ví dụ build form claim).
     */
    List<VerificationQuestion> getQuestionDtos(Long postId);

    /**
     * Xử lý claim: người mất đồ gửi câu trả lời → so với verification_answers.
     * Nếu đạt threshold → trả FullPostDetails (ảnh rõ + thông tin liên hệ).
     * Nếu không → trả rejected với score.
     */
    ClaimResponse claim(Long postId, ClaimAnswerRequest request);

    /**
     * Tính điểm 1 câu trả lời của claimer so với đáp án đúng.
     * Trả về 0.0 - 1.0.
     */
    double scoreAnswer(Long verificationId, String claimerAnswer);
}
