package com.sba301.lostandfound.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.lostandfound.dto.OllamaQuestionsResponse;
import com.sba301.lostandfound.dto.OllamaTags;
import com.sba301.lostandfound.entity.CorrectAnswer;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.Verification;
import com.sba301.lostandfound.repository.CorrectAnswerRepository;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.repository.VerificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service chạy NỀN (async) để bổ sung dữ liệu AI cho post.
 *
 * Hỗ trợ 2 flow:
 *  1. enrichDescriptionAsync: sinh mô tả + tags cho post (dùng cho matching bằng text).
 *  2. generateVerificationQuestionsAsync: AI tự sinh câu hỏi + đáp án đúng từ ảnh.
 *     Dùng cho flow "người mất search → thấy bài có ảnh mờ → trả lời câu hỏi → mới xem được ảnh rõ + liên hệ".
 *
 * Tại sao cần class riêng:
 *  - @Async không hoạt động với self-invocation (gọi this.method() từ chính class đó).
 *  - PostServiceImpl đã là @Service có nhiều method, cần tách để proxy AOP xử lý đúng.
 *
 * Vì sao dùng TransactionTemplate thay vì @Transactional:
 *  - @Transactional trên cùng method với @Async: thứ tự proxy không đảm bảo.
 *  - @Transactional trên method riêng + gọi từ chính class: bị self-invocation, không có proxy.
 *  - TransactionTemplate giải quyết cả 2 vấn đề, code rõ ràng.
 *
 * Hợp đồng:
 *  - Method là fire-and-forget. Caller KHÔNG cần đợi kết quả.
 *  - Nếu Ollama lỗi, phương thức tự nuốt exception (đã được ImageAnalysisService đảm bảo).
 */
@Service
public class PostAiEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(PostAiEnrichmentService.class);

    /** Ngưỡng tối thiểu để coi description là "đủ dùng", không cần AI. */
    private static final int MIN_USER_DESC_LENGTH = 30;

    /** Tối đa số câu hỏi lưu lại cho 1 post (kể cả AI sinh nhiều hơn). */
    private static final int MAX_QUESTIONS_PER_POST = 5;

    private final ImageAnalysisService imageAnalysisService;
    private final PostRepository postRepository;
    private final VerificationRepository verificationRepository;
    private final CorrectAnswerRepository correctAnswerRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PostAiEnrichmentService(
        ImageAnalysisService imageAnalysisService,
        PostRepository postRepository,
        VerificationRepository verificationRepository,
        CorrectAnswerRepository correctAnswerRepository,
        ObjectMapper objectMapper,
        PlatformTransactionManager transactionManager
    ) {
        this.imageAnalysisService = imageAnalysisService;
        this.postRepository = postRepository;
        this.verificationRepository = verificationRepository;
        this.correctAnswerRepository = correctAnswerRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ============================================================
    // Flow 1: Enrich description + tags (dùng cho matching)
    // ============================================================

    /**
     * Fire-and-forget: enrich post với AI description + tags.
     * Giữ lại để phục vụ matching bằng text.
     */
    @Async("aiAnalysisExecutor")
    public CompletableFuture<Optional<OllamaTags>> enrichDescriptionAsync(
        Long postId, String imageUrl, String userDescription
    ) {
        Optional<OllamaTags> result = Optional.empty();
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                return CompletableFuture.completedFuture(result);
            }
            result = imageAnalysisService.analyzeImage(imageUrl, userDescription);
            if (result.isPresent()) {
                final OllamaTags tags = result.get();
                transactionTemplate.executeWithoutResult(status ->
                    applyDescriptionToPost(postId, tags, userDescription)
                );
            }
        } catch (RuntimeException exception) {
            log.warn("AI description enrichment failed for post {}: {}", postId, exception.getMessage());
        }
        return CompletableFuture.completedFuture(result);
    }

    private void applyDescriptionToPost(Long postId, OllamaTags tags, String userDescription) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post {} vanished before AI enrichment could save", postId);
            return;
        }
        Post post = postOpt.get();

        post.setAiDescription(tags.description());
        post.setAiEnrichedAt(LocalDateTime.now());
        post.setAiTags(String.join(",", tags.tags()));

        boolean userDescTooShort = userDescription == null
            || userDescription.trim().length() < MIN_USER_DESC_LENGTH;
        if (userDescTooShort) {
            post.setDescription(tags.description());
            log.info("Replaced empty user description with AI description for post {}", postId);
        } else {
            String merged = post.getDescription() + "\n\n[AI mô tả thêm] " + tags.description();
            post.setDescription(merged);
        }

        postRepository.save(post);
        log.info("Saved AI description enrichment for post {}: tags={}", postId, tags.tags());
    }

    // ============================================================
    // Flow 2: Generate verification questions + auto correct answers
    // AI tự sinh câu hỏi + đáp án đúng → lưu vào verifications + correct_answers.
    // Dùng cho flow verify claim (người mất search → ảnh mờ → trả lời → ảnh rõ).
    // ============================================================

    /**
     * Fire-and-forget: AI sinh câu hỏi xác minh TỪ ẢNH (không cần chủ post trả lời thủ công).
     * Mỗi câu hỏi đi kèm đáp án đúng do AI nhìn thấy trong ảnh.
     *
     * Flow:
     *  1. Gọi Ollama + Qwen-VL sinh 3-5 câu hỏi (mỗi câu có sẵn answer).
     *  2. Xoá câu hỏi cũ (nếu có) của post này.
     *  3. Lưu câu hỏi mới + tự tạo {@link CorrectAnswer} cho mỗi câu.
     *
     * Defensive: nếu Ollama lỗi hoặc trả empty, post vẫn tồn tại bình thường,
     * chỉ là chưa có câu hỏi xác minh.
     */
    @Async("aiAnalysisExecutor")
    public CompletableFuture<Integer> generateVerificationQuestionsAsync(
        Long postId, String imageUrl, String userDescription
    ) {
        int saved = 0;
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                return CompletableFuture.completedFuture(0);
            }
            Optional<OllamaQuestionsResponse> result =
                imageAnalysisService.generateQuestions(imageUrl, userDescription);
            if (result.isPresent() && result.get().questions() != null) {
                final OllamaQuestionsResponse resp = result.get();
                saved = transactionTemplate.execute(status ->
                    saveQuestionsAndAnswersToPost(postId, resp)
                );
            } else {
                log.info("No questions generated for post {} (Ollama empty or error)", postId);
            }
        } catch (RuntimeException exception) {
            log.warn("AI question generation failed for post {}: {}", postId, exception.getMessage());
        }
        return CompletableFuture.completedFuture(saved);
    }

    /**
     * Lưu câu hỏi + đáp án AI sinh vào DB.
     * - Xoá câu hỏi cũ (cascade sẽ xoá luôn correct_answers nhờ FK).
     * - Sắp xếp theo important_point giảm dần, lấy tối đa MAX_QUESTIONS_PER_POST.
     * - Mỗi câu: 1 Verification + 1 CorrectAnswer.
     */
    private int saveQuestionsAndAnswersToPost(Long postId, OllamaQuestionsResponse resp) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post {} vanished before AI questions could save", postId);
            return 0;
        }
        Post post = postOpt.get();

        // Xoá câu hỏi cũ (cascade sẽ xoá luôn correct_answers nhờ FK)
        List<Verification> existing = verificationRepository.findByPostIdOrderByQuestionIndexAsc(postId);
        if (!existing.isEmpty()) {
            verificationRepository.deleteAll(existing);
            verificationRepository.flush();
        }

        // Sắp xếp theo important_point giảm dần, lấy tối đa MAX_QUESTIONS_PER_POST
        List<OllamaQuestionsResponse.OllamaQuestion> sorted = resp.questions().stream()
            .sorted((a, b) -> Integer.compare(
                b.importantPoint() == null ? 1 : b.importantPoint(),
                a.importantPoint() == null ? 1 : a.importantPoint()))
            .limit(MAX_QUESTIONS_PER_POST)
            .toList();

        int idx = 0;
        for (OllamaQuestionsResponse.OllamaQuestion q : sorted) {
            Verification v = new Verification(
                post,
                q.question(),
                normalizeType(q.type()),
                idx++,
                serializeOptions(q.options()),
                q.importantPoint()
            );
            verificationRepository.save(v);
            verificationRepository.flush();

            // Tự sinh CorrectAnswer từ AI response - đây là "đáp án đúng"
            // mà người mất phải đoán trúng để xem được ảnh rõ + thông tin liên hệ.
            String aiAnswer = q.answer();
            if (aiAnswer == null || aiAnswer.isBlank()) {
                log.warn("AI did not provide answer for question '{}' of post {} - skipping",
                    q.question(), postId);
                continue;
            }
            CorrectAnswer correctAnswer = new CorrectAnswer(v, aiAnswer.trim());
            correctAnswerRepository.save(correctAnswer);
        }
        log.info("Saved {} verification questions (+ auto-correct answers) for post {}",
            sorted.size(), postId);
        return sorted.size();
    }

    private String normalizeType(String raw) {
        if (raw == null) return "TEXT";
        return switch (raw.trim().toUpperCase()) {
            case "MULTIPLE_CHOICE", "CHOICE", "MCQ" -> "MULTIPLE_CHOICE";
            case "BOOLEAN", "YES_NO", "YESNO" -> "BOOLEAN";
            default -> "TEXT";
        };
    }

    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception exception) {
            log.warn("Failed to serialize options, fallback to manual: {}", exception.getMessage());
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) sb.append(',');
                String opt = options.get(i).replace("\"", "\\\"");
                sb.append('"').append(opt).append('"');
            }
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Helper public: deserialize options từ JSON string lưu trong DB.
     */
    public List<String> deserializeOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            log.warn("Failed to deserialize options '{}': {}", json, exception.getMessage());
            return List.of();
        }
    }

    /**
     * Lưu câu hỏi xác minh tự tạo hoặc chỉnh sửa từ frontend.
     */
    public void saveCustomQuestions(Long postId, String customQuestionsJson) {
        if (customQuestionsJson == null || customQuestionsJson.isBlank()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            try {
                Optional<Post> postOpt = postRepository.findById(postId);
                if (postOpt.isEmpty()) {
                    log.warn("Post {} vanished before custom questions could save", postId);
                    return;
                }
                Post post = postOpt.get();

                List<OllamaQuestionsResponse.OllamaQuestion> customQuestions = objectMapper.readValue(
                    customQuestionsJson, new TypeReference<List<OllamaQuestionsResponse.OllamaQuestion>>() {}
                );

                if (customQuestions == null || customQuestions.isEmpty()) {
                    return;
                }

                // Xóa câu hỏi cũ
                List<Verification> existing = verificationRepository.findByPostIdOrderByQuestionIndexAsc(postId);
                if (!existing.isEmpty()) {
                    verificationRepository.deleteAll(existing);
                    verificationRepository.flush();
                }

                int idx = 0;
                for (OllamaQuestionsResponse.OllamaQuestion q : customQuestions) {
                    Verification v = new Verification(
                        post,
                        q.question(),
                        normalizeType(q.type()),
                        idx++,
                        serializeOptions(q.options()),
                        q.importantPoint() == null ? 1 : q.importantPoint()
                    );
                    verificationRepository.save(v);
                    verificationRepository.flush();

                    String answer = q.answer();
                    if (answer != null && !answer.isBlank()) {
                        CorrectAnswer correctAnswer = new CorrectAnswer(v, answer.trim());
                        correctAnswerRepository.save(correctAnswer);
                    }
                }
                log.info("Saved {} custom verification questions for post {}", customQuestions.size(), postId);
            } catch (Exception e) {
                log.error("Failed to save custom questions for post {}: {}", postId, e.getMessage(), e);
                throw new RuntimeException("Failed to save custom questions", e);
            }
        });
    }
}
