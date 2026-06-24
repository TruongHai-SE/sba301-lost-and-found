package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Câu hỏi xác minh do AI sinh ra từ ảnh.
 * Trả về FE để hiển thị cho user trả lời ngay khi đăng tin.
 *
 * Các loại câu hỏi:
 *  - TEXT:           user nhập text tự do
 *  - MULTIPLE_CHOICE: user chọn 1 trong các options
 *  - BOOLEAN:        câu hỏi có/không
 *
 * Khi user (chủ post) trả lời → lưu vào {@code VerificationAnswer}.
 * Khi claimer khác trả lời → lưu vào {@code ClaimAttemptAnswer}, score so với VerificationAnswer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationQuestion(
    @JsonProperty("id") Long id,
    @JsonProperty("index") Integer index,
    @JsonProperty("question") String question,
    @JsonProperty("type") String type,
    @JsonProperty("options") List<String> options,
    @JsonProperty("hint") String hint,
    @JsonProperty("important_point") Integer importantPoint
) {

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE";
    public static final String TYPE_BOOLEAN = "BOOLEAN";
}
