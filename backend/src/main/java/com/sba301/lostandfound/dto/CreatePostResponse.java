package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.Verification;
import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Builder
public class CreatePostResponse {

    private static final Logger log = LoggerFactory.getLogger(CreatePostResponse.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * Câu hỏi xác minh do AI sinh ra từ ảnh (Approach 1).
     * Trả về cho FE hiển thị ngay sau khi tạo post để user trả lời.
     * Có thể rỗng nếu AI chưa kịp sinh (Ollama chậm / lỗi) - FE sẽ retry.
     */
    private final List<VerificationQuestion> verificationQuestions;

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
            .verificationQuestions(List.of())
            .build();
    }

    /**
     * Convert danh sách Verification entity → List<VerificationQuestion> DTO.
     */
    public static List<VerificationQuestion> toQuestionDtos(List<Verification> verifications) {
        if (verifications == null || verifications.isEmpty()) {
            return List.of();
        }
        return verifications.stream()
            .map(CreatePostResponse::toQuestionDto)
            .toList();
    }

    public static VerificationQuestion toQuestionDto(Verification v) {
        List<String> options = parseOptions(v.getOptions());
        return new VerificationQuestion(
            v.getId(),
            v.getQuestionIndex(),
            v.getQuestion(),
            v.getQuestionType(),
            options,
            null,
            v.getImportantPoint()
        );
    }

    private static List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse options JSON: {}", json);
            return Collections.emptyList();
        }
    }
}
