package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tóm tắt 1 post - bản "preview" công khai, ẢNH MỜ, KHÔNG có thông tin liên hệ.
 * Dùng cho kết quả search: người mất tìm được các tin tương tự, click vào mới cần claim.
 *
 * <p>Có kèm danh sách câu hỏi xác minh để người xem có thể trả lời ngay (flow claim).
 */
public record BlurredPostSummary(
    @JsonProperty("post_id") Long postId,
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("blurred_image_url") String blurredImageUrl,
    @JsonProperty("original_image_url") String originalImageUrl,
    @JsonProperty("type") String type,
    @JsonProperty("event_time") LocalDateTime eventTime,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("location") LocationInfo location,
    @JsonProperty("match_score") Double matchScore,
    @JsonProperty("verification_questions") List<VerificationQuestion> verificationQuestions,
    @JsonProperty("has_verification") Boolean hasVerification,
    @JsonProperty("is_stock_image") Boolean isStockImage
) {
}
