package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.OllamaQuestionsResponse;
import com.sba301.lostandfound.dto.OllamaTags;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service phân tích ảnh bằng Ollama + Qwen-VL.
 *
 * Hỗ trợ 2 chế độ:
 *  1. analyzeImage: mô tả ảnh (description + tags) - dùng cho matching.
 *  2. generateQuestions: sinh câu hỏi xác minh từ ảnh - dùng cho flow
 *     "AI hỏi - User trả lời" (Approach 1).
 */
public interface ImageAnalysisService {

    /**
     * Phân tích ảnh và trả về mô tả + tags. Trả về empty nếu:
     *   - Ollama không khả dụng
     *   - Model trả response rỗng / không parse được
     *   - Ảnh lỗi
     */
    Optional<OllamaTags> analyzeImage(String imageUrl, String userDescription);

    /**
     * Biến thể async, chạy nền để không block request thread.
     */
    CompletableFuture<Optional<OllamaTags>> analyzeImageAsync(
        Long postId, String imageUrl, String userDescription);

    /**
     * Sinh 3-5 câu hỏi xác minh từ ảnh (Approach 1: AI hỏi - User trả lời).
     * Mỗi câu hỏi giúp xác minh chủ sở hữu thật của đồ vật.
     *
     * @param imageUrl        URL ảnh đồ vật
     * @param userDescription Mô tả người đăng cung cấp (context cho AI)
     * @return danh sách câu hỏi, hoặc empty nếu lỗi
     */
    Optional<OllamaQuestionsResponse> generateQuestions(String imageUrl, String userDescription);

    /**
     * Biến thể async cho {@link #generateQuestions}.
     */
    CompletableFuture<Optional<OllamaQuestionsResponse>> generateQuestionsAsync(
        Long postId, String imageUrl, String userDescription);
}
