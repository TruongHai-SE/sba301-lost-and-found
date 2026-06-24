package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.OllamaTags;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
 * Hợp đồng:
 *  - Method async là fire-and-forget. Caller KHÔNG cần đợi kết quả.
 *  - Nếu Ollama lỗi, phương thức tự nuốt exception (đã được ImageAnalysisService đảm bảo).
 */
public interface PostAiEnrichmentService {

    /**
     * Fire-and-forget: enrich post với AI description + tags.
     * Giữ lại để phục vụ matching bằng text.
     */
    CompletableFuture<Optional<OllamaTags>> enrichDescriptionAsync(
        Long postId, String imageUrl, String userDescription);

    /**
     * Fire-and-forget: AI sinh câu hỏi xác minh TỪ ẢNH (không cần chủ post trả lời thủ công).
     * Mỗi câu hỏi đi kèm đáp án đúng do AI nhìn thấy trong ảnh.
     *
     * @return số câu hỏi đã lưu (0 nếu Ollama lỗi/empty).
     */
    CompletableFuture<Integer> generateVerificationQuestionsAsync(
        Long postId, String imageUrl, String userDescription);

    /**
     * Lưu câu hỏi xác minh tự tạo hoặc chỉnh sửa từ frontend.
     */
    void saveCustomQuestions(Long postId, String customQuestionsJson);
}
