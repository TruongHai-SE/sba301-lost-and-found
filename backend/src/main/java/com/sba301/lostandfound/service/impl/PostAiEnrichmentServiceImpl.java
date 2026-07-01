package com.sba301.lostandfound.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.lostandfound.dto.OllamaQuestionsResponse;
import com.sba301.lostandfound.entity.VerificationAnswer;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.Verification;
import com.sba301.lostandfound.repository.VerificationAnswerRepository;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.repository.VerificationRepository;
import com.sba301.lostandfound.service.ImageAnalysisService;
import com.sba301.lostandfound.service.PostAiEnrichmentService;
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
 * Triển khai {@link PostAiEnrichmentService}.
 *
 * Vì sao dùng TransactionTemplate thay vì @Transactional:
 *  - @Transactional trên cùng method với @Async: thứ tự proxy không đảm bảo.
 *  - @Transactional trên method riêng + gọi từ chính class: bị self-invocation, không có proxy.
 *  - TransactionTemplate giải quyết cả 2 vấn đề, code rõ ràng.
 */
@Service
public class PostAiEnrichmentServiceImpl implements PostAiEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(PostAiEnrichmentServiceImpl.class);

    /** Tối đa số câu hỏi lưu lại cho 1 post (kể cả AI sinh nhiều hơn). */
    private static final int MAX_QUESTIONS_PER_POST = 5;

    private final ImageAnalysisService imageAnalysisService;
    private final PostRepository postRepository;
    private final VerificationRepository verificationRepository;
    private final VerificationAnswerRepository verificationAnswerRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PostAiEnrichmentServiceImpl(
        ImageAnalysisService imageAnalysisService,
        PostRepository postRepository,
        VerificationRepository verificationRepository,
        VerificationAnswerRepository verificationAnswerRepository,
        ObjectMapper objectMapper,
        PlatformTransactionManager transactionManager
    ) {
        this.imageAnalysisService = imageAnalysisService;
        this.postRepository = postRepository;
        this.verificationRepository = verificationRepository;
        this.verificationAnswerRepository = verificationAnswerRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ============================================================
    // Flow: Generate verification questions + auto correct answers
    // AI tự sinh câu hỏi + đáp án chuẩn → lưu vào verifications + verification_answers.
    // Dùng cho flow verify claim (người mất search → ảnh mờ → trả lời → ảnh rõ).
    // ============================================================

    @Override
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
     * - Xoá câu hỏi cũ (cascade sẽ xoá luôn verification_answers nhờ FK).
     * - Sắp xếp theo important_point giảm dần, lấy tối đa MAX_QUESTIONS_PER_POST.
     * - Mỗi câu: 1 Verification + 1 VerificationAnswer.
     */
    private int saveQuestionsAndAnswersToPost(Long postId, OllamaQuestionsResponse resp) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post {} vanished before AI questions could save", postId);
            return 0;
        }
        Post post = postOpt.get();

        // Xoá câu hỏi cũ (cascade sẽ xoá luôn verification_answers nhờ FK)
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
            Verification v = Verification.builder()
                .post(post)
                .question(q.question())
                .questionType(normalizeType(q.type()))
                .questionIndex(idx++)
                .options(serializeOptions(q.options()))
                .importantPoint(q.importantPoint())
                .build();
            verificationRepository.save(v);
            verificationRepository.flush();

            // Tự sinh VerificationAnswer từ AI response - đây là "đáp án chuẩn"
            // mà người mất phải đoán trúng để xem được ảnh rõ + thông tin liên hệ.
            String aiAnswer = q.answer();
            if (aiAnswer == null || aiAnswer.isBlank()) {
                log.warn("AI did not provide answer for question '{}' of post {} - skipping",
                    q.question(), postId);
                continue;
            }
            VerificationAnswer verificationAnswer = VerificationAnswer.builder()
                .verification(v)
                .answer(aiAnswer.trim())
                .build();
            verificationAnswerRepository.save(verificationAnswer);
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

    @Override
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
                    Verification v = Verification.builder()
                        .post(post)
                        .question(q.question())
                        .questionType(normalizeType(q.type()))
                        .questionIndex(idx++)
                        .options(serializeOptions(q.options()))
                        .importantPoint(q.importantPoint() == null ? 1 : q.importantPoint())
                        .build();
                    verificationRepository.save(v);
                    verificationRepository.flush();

                    String answer = q.answer();
                    if (answer != null && !answer.isBlank()) {
                        VerificationAnswer verificationAnswer = VerificationAnswer.builder()
                            .verification(v)
                            .answer(answer.trim())
                            .build();
                        verificationAnswerRepository.save(verificationAnswer);
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
