package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Thông tin CHI TIẾT của 1 post - chỉ trả về khi người xem đã "vượt qua" câu
 * hỏi xác minh
 * (tức là đã trả lời đúng các câu hỏi AI sinh từ ảnh).
 *
 * <p>
 * So với {@link BlurredPostSummary}:
 * - Có ảnh RÕ (không blur)
 * - Có thông tin liên hệ (phone, email, tên đầy đủ của chủ post)
 * - Có location chi tiết (street, district, city)
 */
public record FullPostDetails(
                @JsonProperty("post_id") Long postId,
                @JsonProperty("title") String title,
                @JsonProperty("description") String description,
                @JsonProperty("image_url") String imageUrl,
                @JsonProperty("type") String type,
                @JsonProperty("hide_post_type") String hidePostType,
                @JsonProperty("event_time") LocalDateTime eventTime,
                @JsonProperty("status") String status,
                @JsonProperty("created_at") LocalDateTime createdAt,
                @JsonProperty("location") LocationInfo location,
                @JsonProperty("owner") OwnerInfo owner,
                @JsonProperty("verification_score") Double verificationScore) {
        public record OwnerInfo(
                        @JsonProperty("user_id") Long userId,
                        @JsonProperty("full_name") String fullName,
                        @JsonProperty("phone") String phone,
                        @JsonProperty("email") String email) {
        }
}
