package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.dto.BlurredPostSummary;
import com.sba301.lostandfound.dto.ClipMatch;
import com.sba301.lostandfound.dto.FullPostDetails;
import com.sba301.lostandfound.dto.LocationInfo;
import com.sba301.lostandfound.dto.VerificationQuestion;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostType;
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
        boolean stockImage = Boolean.TRUE.equals(post.getIsStockImage());
        String blurredUrl = null;
        if (post.getImage() != null) {
            if (stockImage) {
                // Stock images: always show clear (no blur, they are generic placeholders)
                blurredUrl = post.getImage().getPrivateUrl() != null
                        ? post.getImage().getPrivateUrl()
                        : post.getImage().getUrl();
            } else {
                boolean isLost = post.getType() == PostType.LOST;
                if (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank()) {
                    blurredUrl = isLost ? post.getImage().getPrivateUrl() : post.getImage().getUrl();
                } else {
                    String originalUrl = post.getImage().getUrl();
                    blurredUrl = isLost ? originalUrl : imageBlurringService.blur(originalUrl);
                    if (blurredUrl == null) {
                        blurredUrl = originalUrl;
                    }
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
            !questions.isEmpty(),
            stockImage
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
     * Map Post → FullPostDetails dùng cho GET /posts/{id} (đường xem trực tiếp).
     * - hidePostType = PUBLIC: trả ảnh rõ + đầy đủ thông tin liên hệ (phone, email).
     * - hidePostType = WHEN_MATCH: trả ảnh MỜ + ẩn phone/email của chủ post;
     *   thông tin đầy đủ chỉ được mở ra khi user vượt qua claim flow
     *   (xem {@link VerificationServiceImpl#buildFullDetails}).
     */
//    public FullPostDetails toFullDetails(Post post, Double verificationScore) {
//        boolean isPublic = post.getHidePostType() == null
//            || post.getHidePostType() == HidePostType.PUBLIC;
//
//        String imageUrl = null;
//        if (post.getImage() != null) {
//            if (isPublic) {
//                imageUrl = (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank())
//                    ? post.getImage().getPrivateUrl()
//                    : post.getImage().getUrl();
//            } else {
//                imageUrl = post.getImage().getUrl();
//            }
//        }
//
//        FullPostDetails.OwnerInfo owner = null;
//        if (post.getUser() != null) {
//            if (isPublic) {
//                owner = new FullPostDetails.OwnerInfo(
//                    post.getUser().getId(),
//                    post.getUser().getName(),
//                    post.getUser().getPhone(),
//                    post.getUser().getMail()
//                );
//            } else {
//                owner = new FullPostDetails.OwnerInfo(
//                    post.getUser().getId(),
//                    post.getUser().getName(),
//                    null,
//                    null
//                );
//            }
//        }
//        return new FullPostDetails(
//            post.getId(),
//            post.getTitle(),
//            post.getDescription(),
//            imageUrl,
//            post.getType() == null ? null : post.getType().name(),
//            post.getHidePostType() == null ? null : post.getHidePostType().name(),
//            post.getEventTime(),
//            post.getStatus() == null ? null : post.getStatus().name(),
//            post.getCreatedAt(),
//            LocationInfo.from(post.getLocation()),
//            owner,
//            verificationScore
//        );
//    }
    public FullPostDetails toFullDetails(Post post, Double verificationScore) {
        // 1. Logic xử lý hiển thị ảnh theo hidePostType
        boolean isPublic = post.getHidePostType() == null
                || post.getHidePostType() == HidePostType.PUBLIC;
        boolean stockImage = Boolean.TRUE.equals(post.getIsStockImage());

        String imageUrl = null;
        if (post.getImage() != null) {
            if (stockImage || isPublic) {
                // Stock images OR hidePostType = PUBLIC: Trả ảnh RÕ
                imageUrl = (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank())
                        ? post.getImage().getPrivateUrl()
                        : post.getImage().getUrl();
            } else {
                // hidePostType = WHEN_MATCH: Trả ảnh MỜ
                imageUrl = post.getImage().getUrl();
            }
        }

        // 2. Logic xử lý thông tin User theo postType
        FullPostDetails.OwnerInfo owner = null;
        if (post.getUser() != null) {
            // Kiểm tra xem bài viết là Nhặt được (FOUND) hay Mất (LOST)
            boolean isFoundType = post.getType() != null && "FOUND".equalsIgnoreCase(post.getType().name());

            if (isFoundType) {
                // postType = FOUND: Ẩn phone/email của chủ post (Bất kể ảnh rõ hay mờ)
                owner = new FullPostDetails.OwnerInfo(
                        post.getUser().getId(),
                        post.getUser().getName(),
                        null,
                        null
                );
            } else {
                // postType = LOST: Hiển thị đầy đủ name/phone/email
                owner = new FullPostDetails.OwnerInfo(
                        post.getUser().getId(),
                        post.getUser().getName(),
                        post.getUser().getPhone(),
                        post.getUser().getMail()
                );
            }
        }

        // 3. Map dữ liệu trả về
        return new FullPostDetails(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                imageUrl,
                post.getCategory() != null ? post.getCategory().name() : null,
                post.getTags(),
                post.getType() == null ? null : post.getType().name(),
                post.getHidePostType() == null ? null : post.getHidePostType().name(),
                post.getEventTime(),
                post.getStatus() == null ? null : post.getStatus().name(),
                post.getCreatedAt(),
                LocationInfo.from(post.getLocation()),
                owner,
                verificationScore,
                stockImage
        );
    }
}
