package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sba301.lostandfound.entity.Post;
import java.time.LocalDateTime;

public record PostListResponse(
    @JsonProperty("id") Long id,
    @JsonProperty("title") String title,
    @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("delete_at") LocalDateTime deleteAt,
    @JsonProperty("owner_id") Long ownerId,
    @JsonProperty("owner_phone") String ownerPhone,
    @JsonProperty("location") LocationInfo location
) {
    public static PostListResponse from(Post post) {
        return new PostListResponse(
            post.getId(),
            post.getTitle(),
            post.getType().name(),
            post.getStatus().name(),
            post.getCreatedAt(),
            post.getDeleteAt(),
            post.getUser() != null ? post.getUser().getId() : null,
            post.getUser() != null ? post.getUser().getPhone() : null,
            LocationInfo.from(post.getLocation())
        );
    }
}
