package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClipMatch(
    @JsonProperty("post_id") Long postId,
    @JsonProperty("score") Double score,
    @JsonProperty("human_score") String humanScore,
    @JsonProperty("image_id") Long imageId
) {
}
