package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body của API claim post: người mất đồ gửi câu trả lời cho các câu hỏi xác minh.
 *
 * <p>Nếu đủ số câu trả lời đúng (theo threshold) → server trả về {@link FullPostDetails}
 * (gồm ảnh rõ + thông tin liên hệ). Nếu không → trả 403 với score.
 */
public record ClaimAnswerRequest(
    @NotNull
    @NotEmpty
    @JsonProperty("answers")
    List<Answer> answers
) {
    public record Answer(
        @JsonProperty("question_id") Long questionId,
        @JsonProperty("answer") String answer
    ) {
    }
}
