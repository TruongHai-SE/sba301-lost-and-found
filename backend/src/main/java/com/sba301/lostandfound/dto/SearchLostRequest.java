package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Body của API search (người mất đồ tìm bài "tìm thấy" liên quan).
 * Gửi dưới dạng multipart/form-data gồm ảnh + tuỳ chọn text mô tả.
 *
 * <p>Server sẽ:
 *  1. Upload ảnh (nếu chưa có) hoặc dùng URL
 *  2. Gọi CLIP service để tìm các post tương tự (cross-type: LOST ↔ FOUND)
 *  3. Trả về {@link SearchResponse} gồm các {@link BlurredPostSummary}
 */
public record SearchLostRequest(
    @NotNull
    @JsonProperty("image") MultipartFile image,
    @JsonProperty("description") String description,
    @JsonProperty("top_k") Integer topK,
    @JsonProperty("target_type") String targetType
) {
}
