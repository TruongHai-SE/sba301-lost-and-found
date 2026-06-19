package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body gọi CLIP service /api/v1/search.
 * 1 trong 2 field {@code query_text} / {@code query_image_url} phải có.
 */
public record ClipSearchRequest(
    @JsonProperty("query_text") String queryText,
    @JsonProperty("query_image_url") String queryImageUrl,
    @JsonProperty("target_post_type") String targetPostType,
    @JsonProperty("top_k") Integer topK,
    @JsonProperty("threshold") Double threshold
) {
    public static ClipSearchRequest of(String queryText, String queryImageUrl,
                                      String targetType, int topK) {
        return new ClipSearchRequest(queryText, queryImageUrl, targetType, topK, 0.0);
    }
}
