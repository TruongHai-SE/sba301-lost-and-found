package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClipMatch(
    @JsonProperty("post_id") Long postId,
    @JsonProperty("score") Double score,
    @JsonProperty("human_score") String humanScore,
    @JsonProperty("image_id") Long imageId,
    @JsonProperty("image_url") String imageUrl
) {
    public static ClipMatch withUrl(ClipMatch match, String imageUrl) {
        return new ClipMatch(match.postId, match.score, match.humanScore, match.imageId, imageUrl);
    }
}
