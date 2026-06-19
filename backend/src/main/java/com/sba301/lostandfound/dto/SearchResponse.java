package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Kết quả search: danh sách các post liên quan (dạng BlurredPostSummary).
 * Tất cả ảnh đều MỜ, không có thông tin liên hệ.
 */
public record SearchResponse(
    @JsonProperty("query_type") String queryType,
    @JsonProperty("total") int total,
    @JsonProperty("results") List<BlurredPostSummary> results
) {
}
