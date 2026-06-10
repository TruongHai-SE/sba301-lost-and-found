package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePostResponse {

    private final Long postId;
    private final String title;
    private final String description;
    private final PostType type;
    private final PostStatus status;
    private final HidePostType hidePostType;
    private final LocalDateTime eventTime;
    private final LocalDateTime createAt;
    private final String imageUrl;
    private final Long userId;
    private final LocationInfo location;
    private final List<ClipMatch> matches;

    public static CreatePostResponse from(Post post, List<ClipMatch> matches) {
        return CreatePostResponse.builder()
            .postId(post.getId())
            .title(post.getTitle())
            .description(post.getDescription())
            .type(post.getType())
            .status(post.getStatus())
            .hidePostType(post.getHidePostType())
            .eventTime(post.getEventTime())
            .createAt(post.getCreateAt())
            .imageUrl(post.getImage() == null ? null : post.getImage().getUrl())
            .userId(post.getUser() == null ? null : post.getUser().getId())
            .location(LocationInfo.from(post.getLocation()))
            .matches(matches == null ? List.of() : matches)
            .build();
    }
}
