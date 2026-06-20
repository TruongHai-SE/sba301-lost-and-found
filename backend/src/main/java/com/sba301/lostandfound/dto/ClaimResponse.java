package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kết quả claim post.
 *
 * <p>2 trường hợp:
 *  - {@code approved = true}: trả về {@link FullPostDetails} (ảnh rõ + liên hệ)
 *  - {@code approved = false}: trả về score + message lý do từ chối
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClaimResponse(
    @JsonProperty("approved") boolean approved,
    @JsonProperty("post_id") Long postId,
    @JsonProperty("score") Double score,
    @JsonProperty("threshold") Double threshold,
    @JsonProperty("message") String message,
    @JsonProperty("details") FullPostDetails details
) {

    public static ClaimResponse approved(Long postId, double score, double threshold, FullPostDetails details) {
        return new ClaimResponse(true, postId, score, threshold,
            "Verified successfully", details);
    }

    public static ClaimResponse rejected(Long postId, double score, double threshold, String message) {
        return new ClaimResponse(false, postId, score, threshold, message, null);
    }
}
