package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * User (chủ post) gửi câu trả lời cho các câu hỏi do AI sinh.
 * Mỗi phần tử trong answers: id câu hỏi + text trả lời.
 */
public record SubmitAnswersRequest(
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
