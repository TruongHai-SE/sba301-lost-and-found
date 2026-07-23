package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.dto.ClaimAnswerRequest;
import com.sba301.lostandfound.dto.ClaimResponse;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.dto.FullPostDetails;
import com.sba301.lostandfound.dto.LocationInfo;
import com.sba301.lostandfound.dto.VerificationQuestion;
import com.sba301.lostandfound.dto.VerificationQuestionsResponse;
import com.sba301.lostandfound.entity.VerificationAnswer;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.Verification;
import com.sba301.lostandfound.repository.VerificationAnswerRepository;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.repository.VerificationRepository;
import com.sba301.lostandfound.service.VerificationService;
import com.sba301.lostandfound.util.StringSanitizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementation xử lý câu hỏi xác minh + claim flow.
 *
 * <p>Flow chính (Approach 2: AI hỏi - AI tự trả lời):
 *  1. AI sinh câu hỏi + đáp án chuẩn → lưu verifications + verification_answers (xem {@code PostAiEnrichmentService}).
 *  2. Người mất đồ gửi câu trả lời qua {@link #claim(Long, ClaimAnswerRequest)}.
 *  3. Server score từng câu, tính overall score. Nếu {@code overall >= THRESHOLD} → trả FullPostDetails.
 */
@Service
public class VerificationServiceImpl implements VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationServiceImpl.class);

    /** Threshold overall score để approved claim. 0.6 = 60% trả lời đúng (tính trung bình). */
    private static final double CLAIM_APPROVAL_THRESHOLD = 0.6;

    private final VerificationRepository verificationRepository;
    private final VerificationAnswerRepository verificationAnswerRepository;
    private final PostRepository postRepository;

    public VerificationServiceImpl(
        VerificationRepository verificationRepository,
        VerificationAnswerRepository verificationAnswerRepository,
        PostRepository postRepository
    ) {
        this.verificationRepository = verificationRepository;
        this.verificationAnswerRepository = verificationAnswerRepository;
        this.postRepository = postRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationQuestionsResponse getQuestionsForPost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId);
        }
        List<Verification> verifications = verificationRepository.findByPostIdOrderByQuestionIndexAsc(postId);
        if (verifications.isEmpty()) {
            return VerificationQuestionsResponse.empty(postId);
        }
        List<VerificationQuestion> dtos = CreatePostResponse.toQuestionDtos(verifications);
        return VerificationQuestionsResponse.pending(postId, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationQuestion> getQuestionDtos(Long postId) {
        List<Verification> verifications = verificationRepository.findByPostIdOrderByQuestionIndexAsc(postId);
        return CreatePostResponse.toQuestionDtos(verifications);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse claim(Long postId, ClaimAnswerRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Post not found: " + postId));

        List<Verification> verifications =
            verificationRepository.findByPostIdOrderByQuestionIndexAsc(postId);
        if (verifications.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Post chưa có câu hỏi xác minh (AI chưa sinh hoặc đã bị xoá)"
            );
        }

        // Map: verificationId → claimer answer (lấy câu trả lời đầu tiên cho mỗi câu hỏi)
        Map<Long, String> answerById = new HashMap<>();
        for (ClaimAnswerRequest.Answer a : request.answers()) {
            if (a == null || a.questionId() == null) continue;
            // Nếu claimer gửi nhiều lần cùng questionId, lấy câu đầu
            answerById.putIfAbsent(a.questionId(),
                a.answer() == null ? "" : a.answer().trim());
        }

        // Score từng câu
        double total = 0.0;
        int scored = 0;
        for (Verification v : verifications) {
            String claimer = answerById.get(v.getId());
            if (claimer == null) {
                // Không trả lời câu này → 0 điểm
                scored++;
                continue;
            }
            double s = scoreAnswer(v.getId(), claimer);
            total += s;
            scored++;
        }
        double overall = scored == 0 ? 0.0 : total / scored;
        log.info("Claim post {}: score={} (threshold={})", postId, overall, CLAIM_APPROVAL_THRESHOLD);

        if (overall >= CLAIM_APPROVAL_THRESHOLD) {
            FullPostDetails details = buildFullDetails(post, overall);
            return ClaimResponse.approved(postId, overall, CLAIM_APPROVAL_THRESHOLD, details);
        }
        return ClaimResponse.rejected(
            postId, overall, CLAIM_APPROVAL_THRESHOLD,
            "Câu trả lời chưa đủ chính xác. Hãy xem lại ảnh mờ và mô tả chi tiết hơn."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public double scoreAnswer(Long verificationId, String claimerAnswer) {
        if (claimerAnswer == null || claimerAnswer.isBlank()) {
            return 0.0;
        }
        Optional<VerificationAnswer> correctOpt =
            verificationAnswerRepository.findFirstByVerificationId(verificationId);
        if (correctOpt.isEmpty()) {
            return 0.0;
        }
        String correct = correctOpt.get().getAnswer();

        Optional<Verification> vOpt = verificationRepository.findById(verificationId);
        if (vOpt.isEmpty()) {
            return 0.0;
        }
        String type = vOpt.get().getQuestionType();

        return switch (type == null ? "TEXT" : type.toUpperCase(Locale.ROOT)) {
            case "BOOLEAN" -> scoreBoolean(correct, claimerAnswer);
            case "MULTIPLE_CHOICE" -> scoreMultipleChoice(correct, claimerAnswer);
            default -> scoreText(correct, claimerAnswer);
        };
    }

    private double scoreBoolean(String correct, String claimer) {
        String c = normalizeBoolean(correct);
        String u = normalizeBoolean(claimer);
        return c.equals(u) ? 1.0 : 0.0;
    }

    private String normalizeBoolean(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "có", "yes", "y", "true", "1" -> "yes";
            case "không", "no", "n", "false", "0" -> "no";
            default -> s;
        };
    }

    private double scoreMultipleChoice(String correct, String claimer) {
        return normalize(correct).equals(normalize(claimer)) ? 1.0 : 0.0;
    }

    private double scoreText(String correct, String claimer) {
        String c = normalize(correct);
        String u = normalize(claimer);
        if (c.isEmpty() || u.isEmpty()) return 0.0;
        if (c.equals(u)) return 1.0;
        if (c.contains(u) || u.contains(c)) return 0.8;
        return jaccardSimilarity(c, u);
    }

    private String normalize(String s) {
        return StringSanitizer.sanitizeSearchText(s);
    }

    private double jaccardSimilarity(String a, String b) {
        java.util.Set<String> setA = new java.util.HashSet<>(java.util.Arrays.asList(a.split(" ")));
        java.util.Set<String> setB = new java.util.HashSet<>(java.util.Arrays.asList(b.split(" ")));
        setA.removeIf(String::isEmpty);
        setB.removeIf(String::isEmpty);
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;
        java.util.Set<String> intersection = new java.util.HashSet<>(setA);
        intersection.retainAll(setB);
        java.util.Set<String> union = new java.util.HashSet<>(setA);
        union.addAll(setB);
        return (double) intersection.size() / union.size();
    }

    /**
     * Map Post entity → FullPostDetails (inline để tránh vòng phụ thuộc circular với PostMapper).
     * CHỈ gọi khi claim đã được phê duyệt (score >= threshold).
     */
    private FullPostDetails buildFullDetails(Post post, Double verificationScore) {
        String imageUrl = null;
        if (post.getImage() != null) {
            imageUrl = (post.getImage().getPrivateUrl() != null && !post.getImage().getPrivateUrl().isBlank())
                ? post.getImage().getPrivateUrl()
                : post.getImage().getUrl();
        }
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
            Boolean.TRUE.equals(post.getIsStockImage())
        );
    }
}

