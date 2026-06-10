package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmbedTextRequest(
    @JsonProperty("post_id") Long postId,
    @JsonProperty("text") String text,
    @JsonProperty("translate") boolean translate,
    @JsonProperty("post_type") String postType
) {
}
