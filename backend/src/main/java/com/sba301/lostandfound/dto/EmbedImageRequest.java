package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmbedImageRequest(
    @JsonProperty("post_id") Long postId,
    @JsonProperty("image_url") String imageUrl,
    @JsonProperty("image_id") Long imageId,
    @JsonProperty("post_type") String postType
) {
}
