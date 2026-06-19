package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.dto.BlurredPostSummary;
import com.sba301.lostandfound.dto.ClipMatch;
import com.sba301.lostandfound.dto.FullPostDetails;
import com.sba301.lostandfound.dto.LocationInfo;
import com.sba301.lostandfound.dto.VerificationQuestion;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.service.VerificationService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Helper map Post entity → DTOs (BlurredPostSummary, FullPostDetails).
 * Tách ra để tránh lặp code giữa SearchService và VerificationService.
 */
@Component
public class PostMapper {

    private final ImageBlurringService imageBlurringService;
    private final VerificationService verificationService;

    public PostMapper(
        ImageBlurringService imageBlurringService,
        VerificationService verificationService
    ) {
        this.imageBlurringService = imageBlurringService;
        this.verificationService = verificationService;
    }

    /**
     * Map Post → BlurredPostSummary (ảnh mờ, không có thông tin liên hệ).
     * @param post     entity post
     * @param score    match score từ CLIP (optional, có thể null)
     */
    public BlurredPostSummary toBlurredSummary(Post post, Double score) {
        String originalUrl = post.getImage() == null ? null : post.getImage().getUrl();
        String blurredUrl = imageBlurringService.blur(originalUrl);
        if (blurredUrl == null) {
            blurredUrl = originalUrl;
        }
        List<VerificationQuestion> questions = verificationService.getQuestionDtos(post.getId());
        return new BlurredPostSummary(
            post.getId(),
            post.getTitle(),
            post.getDescription(),
            blurredUrl,
            originalUrl,
            post.getType() == null ? null : post.getType().name(),
            post.getEventTime(),
            post.getCreateAt(),
            LocationInfo.from(post.getLocation()),
            score,
            questions,
            !questions.isEmpty()
        );
    }

    /**
     * Map Post → BlurredPostSummary từ ClipMatch (lấy score từ match).
     */
    public BlurredPostSummary toBlurredSummary(Post post, ClipMatch match) {
        return toBlurredSummary(post, match == null ? null : match.score());
    }

    /**
     * Map Post → FullPostDetails (ảnh rõ, có thông tin liên hệ).
     * CHỈ dùng khi user đã vượt qua claim flow.
     */
    public FullPostDetails toFullDetails(Post post, Double verificationScore) {
        String imageUrl = post.getImage() == null ? null : post.getImage().getUrl();
        FullPostDetails.OwnerInfo owner = null;
        if (post.getUser() != null) {
            owner = new FullPostDetails.OwnerInfo(
                post.getUser().getId(),
                post.getUser().getName(),
                post.getUser().getPhone(),
                post.getUser().getMail()
            );
        }
        return new FullPostDetails(
            post.getId(),
            post.getTitle(),
            post.getDescription(),
            imageUrl,
            post.getType() == null ? null : post.getType().name(),
            post.getEventTime(),
            post.getStatus() == null ? null : post.getStatus().name(),
            post.getCreateAt(),
            LocationInfo.from(post.getLocation()),
            owner,
            verificationScore
        );
    }
}
