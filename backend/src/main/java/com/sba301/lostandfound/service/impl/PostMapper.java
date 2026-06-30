package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.dto.BlurredPostSummary;
import com.sba301.lostandfound.dto.ClipMatch;
import com.sba301.lostandfound.dto.FullPostDetails;
import com.sba301.lostandfound.dto.LocationInfo;
import com.sba301.lostandfound.dto.VerificationQuestion;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.HidePostType;
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
        String blurredUrl = null;
        if (post.getImage() != null) {
            if (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank()) {
                // New logic: url stores the blurred image
                blurredUrl = post.getImage().getUrl();
            } else {
                // Backward compatibility: url stores the clear image, privateUrl is null
                String originalUrl = post.getImage().getUrl();
                blurredUrl = imageBlurringService.blur(originalUrl);
                if (blurredUrl == null) {
                    blurredUrl = originalUrl;
                }
            }
        }
        List<VerificationQuestion> questions = verificationService.getQuestionDtos(post.getId());
        return new BlurredPostSummary(
            post.getId(),
            post.getTitle(),
            post.getDescription(),
            blurredUrl,
            null, // Hiding original image URL from public search/summary to prevent leak
            post.getType() == null ? null : post.getType().name(),
            post.getEventTime(),
            post.getCreatedAt(),
            LocationInfo.from(post.getLocation()),
            score,
            questions,
            !questions.isEmpty()
        );
    }

    /**
     * Map Post -> BlurredPostSummary from ClipMatch (scaled human score).
     */
    public BlurredPostSummary toBlurredSummary(Post post, ClipMatch match) {
        if (match == null) {
            return toBlurredSummary(post, (Double) null);
        }
        Double parsedScore = null;
        if (match.humanScore() != null) {
            try {
                String clean = match.humanScore().replace("%", "").trim();
                parsedScore = Double.parseDouble(clean) / 100.0;
            } catch (Exception e) {
                parsedScore = match.score();
            }
        } else {
            parsedScore = match.score();
        }
        return toBlurredSummary(post, parsedScore);
    }

    /**
     * Map Post → FullPostDetails (ảnh rõ, có thông tin liên hệ).
     * CHỈ dùng khi user đã vượt qua claim flow.
     */
    public FullPostDetails toFullDetails(Post post, Double verificationScore) {
        String imageUrl = null;
        if (post.getImage() != null) {
            imageUrl = (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank())
                ? post.getImage().getPrivateUrl()
                : post.getImage().getUrl();
        }
        FullPostDetails.OwnerInfo owner = null;
        if (post.getUser() != null) {
            boolean isPublic = post.getHidePostType() == HidePostType.PUBLIC;
            owner = new FullPostDetails.OwnerInfo(
                post.getUser().getId(),
                post.getUser().getName(),
                isPublic ? post.getUser().getPhone() : null,
                isPublic ? post.getUser().getMail() : null
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
            post.getCreatedAt(),
            LocationInfo.from(post.getLocation()),
            owner,
            verificationScore
        );
    }
}
