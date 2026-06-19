package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response từ Ollama POST /api/generate khi gửi kèm ảnh.
 * Chỉ lấy vài field cần thiết, phần còn lại bỏ qua.
 *
 * Ví dụ response thật:
 * {
 *   "model": "qwen2.5vl",
 *   "response": "Đây là một chiếc ví da nâu...",
 *   "total_duration": 1234567890,
 *   "done": true
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VisionDescription(
    String model,
    String response,
    @JsonProperty("total_duration") Long totalDuration,
    Boolean done
) {
}
