package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Kết quả parse từ response của Ollama khi sinh câu hỏi xác minh.
 * AI phải trả về JSON array theo format định sẵn.
 *
 * <p>Mỗi câu hỏi giờ đi kèm {@code answer} - đáp án đúng mà AI "nhìn thấy" trong ảnh.
 * Đáp án này được lưu vào {@code correct_answers} để dùng cho flow verify claim:
 * khi người mất muốn xem ảnh rõ + thông tin liên hệ, họ phải trả lời đúng các câu hỏi này.
 */
public record OllamaQuestionsResponse(
    @JsonProperty("questions") List<OllamaQuestion> questions
) {
    public record OllamaQuestion(
        @JsonProperty("question") String question,
        @JsonProperty("type") String type,
        @JsonProperty("options") List<String> options,
        @JsonProperty("hint") String hint,
        @JsonProperty("answer") String answer,
        @JsonProperty("important_point") Integer importantPoint
    ) {
    }
}
