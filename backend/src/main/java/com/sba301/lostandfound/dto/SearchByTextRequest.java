package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body của API search bằng text.
 */
public record SearchByTextRequest(
    @JsonProperty("text") String text,
    @JsonProperty("top_k") Integer topK,
    @JsonProperty("target_type") String targetType
) {
}
