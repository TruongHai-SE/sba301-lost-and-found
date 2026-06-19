package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response trả về cho FE: danh sách câu hỏi cần user trả lời.
 */
public record VerificationQuestionsResponse(
    @JsonProperty("post_id") Long postId,
    @JsonProperty("questions") List<VerificationQuestion> questions,
    @JsonProperty("status") String status,
    @JsonProperty("message") String message
) {
    public static VerificationQuestionsResponse pending(Long postId, List<VerificationQuestion> questions) {
        return new VerificationQuestionsResponse(
            postId, questions, "PENDING",
            "Vui lòng trả lời các câu hỏi sau để hoàn tất mô tả đồ vật"
        );
    }

    public static VerificationQuestionsResponse empty(Long postId) {
        return new VerificationQuestionsResponse(
            postId, List.of(), "NOT_GENERATED",
            "AI chưa sinh câu hỏi (có thể Ollama chưa sẵn sàng). User có thể tiếp tục dùng mô tả thủ công"
        );
    }
}
