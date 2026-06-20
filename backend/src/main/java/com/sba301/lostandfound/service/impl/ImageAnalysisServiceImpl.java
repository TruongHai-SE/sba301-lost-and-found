package com.sba301.lostandfound.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.lostandfound.client.OllamaClient;
import com.sba301.lostandfound.dto.OllamaQuestionsResponse;
import com.sba301.lostandfound.dto.OllamaTags;
import com.sba301.lostandfound.dto.VisionDescription;
import com.sba301.lostandfound.service.ImageAnalysisService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation gọi Ollama + Qwen-VL để phân tích ảnh.
 *
 * Hỗ trợ 2 chế độ:
 *  1. analyzeImage: sinh mô tả + tags từ ảnh
 *  2. generateQuestions: sinh câu hỏi xác minh từ ảnh (Approach 1: AI hỏi - User trả lời)
 *
 * Defensive:
 *  - Bất kỳ exception nào (timeout, 500, parse fail) đều trả Optional.empty()
 *  - Phương thức này KHÔNG BAO GIỜ throw, caller yên tâm dùng.
 */
@Service
public class ImageAnalysisServiceImpl implements ImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisServiceImpl.class);

    // === Prompt cho chế độ analyzeImage (mô tả + tags) ===

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        Bạn là trợ lý mô tả đồ vật trong ảnh cho hệ thống tìm đồ thất lạc.
        Hãy quan sát ảnh và trả lời ĐÚNG theo format sau (không thêm giải thích):

        DESCRIPTION: <mô tả ngắn 1-2 câu bằng tiếng Việt về loại đồ vật, màu sắc, chất liệu, đặc điểm nhận dạng>
        TAGS: <danh sách 3-7 từ khoá ngắn tiếng Việt, phân cách bằng dấu phẩy, ví dụ: ví,da,nâu,mini>

        Yêu cầu:
        - Chỉ mô tả những gì nhìn thấy được, KHÔNG bịa thêm
        - TAGS viết thường, không dấu cách trong từ
        - Bỏ qua bối cảnh nền, chỉ tập trung vào vật chính
        """;

    // === Prompt cho chế độ generateQuestions (câu hỏi xác minh) ===

    private static final String QUESTIONS_PROMPT_TEMPLATE = """
        Bạn là trợ lý AI cho hệ thống tìm đồ thất lạc. Hãy quan sát ảnh và sinh 3-5 câu hỏi
        xác minh (verification questions) để phân biệt chủ sở hữu THẬT của đồ vật với người
        nhặt được / người khác.

        Yêu cầu:
        - Mỗi câu hỏi phải dựa vào chi tiết CỤ THỂ nhìn thấy trong ảnh (không hỏi chung chung)
        - Câu trả lời KHÔNG thể đoán được nếu không thực sự là chủ sở hữu
        - Ưu tiên câu hỏi về: nội dung bên trong, vết xước/vết bẩn đặc trưng, phụ kiện đi kèm,
          thông tin cá nhân (tên trên đồ), hình dáng riêng
        - TRÁNH hỏi thông tin không nhìn thấy được (ví dụ: giá tiền, ngày mua)
        - Tối đa 5 câu, tối thiểu 3 câu

        QUAN TRỌNG: trả lời CHỈ bằng JSON thuần, KHÔNG markdown, KHÔNG giải thích, theo schema:

        {
          "questions": [
            {
              "question": "Câu hỏi bằng tiếng Việt",
              "type": "TEXT" | "MULTIPLE_CHOICE" | "BOOLEAN",
              "options": ["opt1","opt2","opt3"],
              "hint": "Gợi ý ngắn (nếu cần)",
              "answer": "Đáp án đúng CHÍNH XÁC mà bạn nhìn thấy trong ảnh",
              "important_point": 1-3
            }
          ]
        }

        Trong đó:
        - "type": "TEXT" cho câu nhập tự do, "BOOLEAN" cho câu có/không, "MULTIPLE_CHOICE" cho câu chọn 1
        - "options": chỉ cần thiết cho MULTIPLE_CHOICE (2-4 lựa chọn); các loại khác để []
        - "answer": BẮT BUỘC. Phải là giá trị mà chủ sở hữu THẬT sẽ trả lời đúng dựa trên ảnh.
            + BOOLEAN: "có" hoặc "không" (không phải true/false)
            + MULTIPLE_CHOICE: phải trùng CHÍNH XÁC một phần tử trong "options"
            + TEXT: mô tả ngắn gọn chi tiết nhìn thấy được trong ảnh
        - "important_point": 1 = bình thường, 2 = quan trọng, 3 = rất quan trọng (để chấm điểm)
        """;

    private static final Pattern DESC_PATTERN =
        Pattern.compile("DESCRIPTION\\s*[:：]\\s*(.+?)(?=\\n\\s*TAGS\\s*[:：]|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAGS_PATTERN =
        Pattern.compile("TAGS\\s*[:：]\\s*(.+?)$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public ImageAnalysisServiceImpl(OllamaClient ollamaClient, ObjectMapper objectMapper) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // Chế độ 1: analyzeImage (mô tả + tags)
    // ============================================================

    @Override
    public Optional<OllamaTags> analyzeImage(String imageUrl, String userDescription) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return Optional.empty();
        }

        String prompt = buildPrompt(userDescription);
        VisionDescription result = ollamaClient.describeImage(imageUrl, prompt);
        if (result == null || result.response() == null || result.response().isBlank()) {
            return Optional.empty();
        }

        return parseResponse(result.response());
    }

    @Async("aiAnalysisExecutor")
    @Override
    public CompletableFuture<Optional<OllamaTags>> analyzeImageAsync(
        Long postId, String imageUrl, String userDescription) {
        log.info("Async AI analysis started for postId={}", postId);
        Optional<OllamaTags> result = analyzeImage(imageUrl, userDescription);
        log.info("Async AI analysis finished for postId={}, hasResult={}", postId, result.isPresent());
        return CompletableFuture.completedFuture(result);
    }

    private String buildPrompt(String userDescription) {
        String userCtx = (userDescription == null || userDescription.isBlank())
            ? ""
            : "\n\nNgười đăng mô tả thêm: " + userDescription;
        return SYSTEM_PROMPT_TEMPLATE + userCtx;
    }

    private Optional<OllamaTags> parseResponse(String raw) {
        try {
            String description = null;
            List<String> tags = List.of();

            Matcher descMatcher = DESC_PATTERN.matcher(raw);
            if (descMatcher.find()) {
                description = descMatcher.group(1).trim();
            }

            Matcher tagsMatcher = TAGS_PATTERN.matcher(raw);
            if (tagsMatcher.find()) {
                String tagsLine = tagsMatcher.group(1).trim();
                tags = Arrays.stream(tagsLine.split("[,，;；]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase().replaceAll("\\s+", "_"))
                    .distinct()
                    .limit(7)
                    .toList();
            }

            if (description == null || description.isBlank()) {
                description = raw.replaceAll("(?i)TAGS\\s*[:：].*$", "").trim();
            }
            if (description.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new OllamaTags(description, tags));
        } catch (RuntimeException exception) {
            log.warn("Failed to parse Ollama response: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    // ============================================================
    // Chế độ 2: generateQuestions (câu hỏi xác minh - Approach 1)
    // ============================================================

    @Override
    public Optional<OllamaQuestionsResponse> generateQuestions(String imageUrl, String userDescription) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return Optional.empty();
        }

        String prompt = buildQuestionsPrompt(userDescription);
        VisionDescription result = ollamaClient.describeImage(imageUrl, prompt);
        if (result == null || result.response() == null || result.response().isBlank()) {
            return Optional.empty();
        }

        return parseQuestionsResponse(result.response());
    }

    @Async("aiAnalysisExecutor")
    @Override
    public CompletableFuture<Optional<OllamaQuestionsResponse>> generateQuestionsAsync(
        Long postId, String imageUrl, String userDescription) {
        log.info("Async AI question generation started for postId={}", postId);
        Optional<OllamaQuestionsResponse> result = generateQuestions(imageUrl, userDescription);
        log.info("Async AI question generation finished for postId={}, hasResult={}",
            postId, result.isPresent());
        return CompletableFuture.completedFuture(result);
    }

    private String buildQuestionsPrompt(String userDescription) {
        String userCtx = (userDescription == null || userDescription.isBlank())
            ? ""
            : "\n\nNgười đăng mô tả: " + userDescription;
        return QUESTIONS_PROMPT_TEMPLATE + userCtx;
    }

    /**
     * Parse JSON response từ Ollama. Một số model vẫn trả markdown ```json ... ```,
     * nên ta cần strip markdown wrapper trước khi parse.
     */
    private Optional<OllamaQuestionsResponse> parseQuestionsResponse(String raw) {
        try {
            String json = stripMarkdownJson(raw);
            OllamaQuestionsResponse parsed = objectMapper.readValue(json, OllamaQuestionsResponse.class);
            if (parsed.questions() == null || parsed.questions().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parsed);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse Ollama questions JSON: {}", exception.getMessage());
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("Unexpected error parsing questions: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Strip markdown code fence ```json ... ``` nếu model trả kèm.
     */
    private String stripMarkdownJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence > 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }
}
