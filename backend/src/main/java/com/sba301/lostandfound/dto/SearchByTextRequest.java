package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sba301.lostandfound.entity.enums.Category;
import com.sba301.lostandfound.entity.enums.PostStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Body của API search bằng text, kết hợp filter.
 *
 * <p>Các trường filter (category, district, date, time, tag, status) đều optional.
 * Khi truyền, các filter sẽ được áp lên kết quả CLIP semantic search để thu hẹp.
 */
public record SearchByTextRequest(
    @JsonProperty("text") String text,
    @JsonProperty("top_k") Integer topK,
    @JsonProperty("target_type") String targetType,
    @JsonProperty("category") Category category,
    @JsonProperty("district") String district,
    @JsonProperty("date") LocalDate date,
    @JsonProperty("time") LocalTime time,
    @JsonProperty("tag") String tag,
    @JsonProperty("status") PostStatus status
) {
}