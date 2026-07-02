package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.HidePostType;
import java.time.LocalDateTime;

public record PostListResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title,
        @JsonProperty("type") String type,
        @JsonProperty("status") String status,
        @JsonProperty("hide_post_type") String hidePostType,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("delete_at") LocalDateTime deleteAt,
        @JsonProperty("owner_id") Long ownerId,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("location") LocationInfo location) {
    public static PostListResponse from(Post post) {
        boolean isPublic = post.getHidePostType() == null
                || post.getHidePostType() == HidePostType.PUBLIC;

        String imageUrl = null;
        if (post.getImage() != null) {
            if (isPublic) {
                imageUrl = (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank())
                        ? post.getImage().getPrivateUrl()
                        : post.getImage().getUrl();
            } else {
                imageUrl = post.getImage().getUrl();
            }
        }


        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getType().name(),
                post.getStatus().name(),
                post.getHidePostType() == null ? null : post.getHidePostType().name(),
                post.getCreatedAt(),
                post.getDeleteAt(),
                post.getUser() != null ? post.getUser().getId() : null,
                imageUrl,
                LocationInfo.from(post.getLocation()));
    }
}
