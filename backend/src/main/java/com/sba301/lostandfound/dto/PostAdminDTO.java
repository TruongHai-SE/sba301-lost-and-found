package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sba301.lostandfound.entity.Post;
import java.time.LocalDateTime;

public record PostAdminDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("title") String title,
    @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @JsonProperty("create_at") LocalDateTime createAt,
    @JsonProperty("delete_at") LocalDateTime deleteAt,
    @JsonProperty("owner_id") Long ownerId,
    @JsonProperty("owner_phone") String ownerPhone
) {
    public static PostAdminDTO from(Post post) {
        return new PostAdminDTO(
            post.getId(),
            post.getTitle(),
            post.getType().name(),
            post.getStatus().name(),
            post.getCreateAt(),
            post.getDeleteAt(),
            post.getUser() != null ? post.getUser().getId() : null,
            post.getUser() != null ? post.getUser().getPhone() : null
        );
    }
}
