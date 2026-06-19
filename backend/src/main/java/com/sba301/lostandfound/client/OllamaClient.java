package com.sba301.lostandfound.client;

import com.sba301.lostandfound.config.OllamaProperties;
import com.sba301.lostandfound.dto.VisionDescription;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * REST client gọi Ollama API. Hỗ trợ 2 chức năng chính:
 *
 * 1. describeImage(imageUrl, prompt)  – gửi ảnh + prompt cho model vision-language
 *    (Qwen2.5-VL, LLaVA, llama3.2-vision, ...) để sinh mô tả vật thể.
 *
 * 2. getStatus() – ping /api/tags để kiểm tra Ollama có sống không (dùng cho health check).
 *
 * Lưu ý: Ollama nhận ảnh qua field "images" dạng list string. Mỗi phần tử có thể là:
 *   - URL (Ollama tự download về)
 *   - Base64 string
 *   Ở đây ta dùng URL vì ảnh đã upload Cloudinary, public accessible.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(RestClient ollamaRestClient, OllamaProperties properties) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
    }

    /**
     * Gửi ảnh + prompt cho Ollama vision model, trả về text mô tả.
     *
     * @param imageUrl URL công khai của ảnh (Cloudinary URL)
     * @param prompt   system + user prompt hướng dẫn model trả lời
     * @return response text hoặc null nếu lỗi
     */
    public VisionDescription describeImage(String imageUrl, String prompt) {
        if (!properties.enabled()) {
            log.debug("Ollama is disabled by config, skip describeImage");
            return null;
        }

        String base64Image = downloadAndEncodeBase64(imageUrl);
        if (base64Image == null) {
            log.warn("Could not convert image to base64, skipping Ollama analysis");
            return null;
        }

        // Payload theo Ollama API: https://github.com/ollama/ollama/blob/main/docs/api.md
        Map<String, Object> request = Map.of(
            "model", properties.visionModel(),
            "prompt", prompt,
            "images", List.of(base64Image),
            "stream", false
        );

        try {
            VisionDescription response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VisionDescription.class);

            if (response != null && response.response() != null) {
                log.info("Ollama ({}) processed image in {} ns",
                    properties.visionModel(),
                    response.totalDuration());
            }
            return response;
        } catch (RuntimeException exception) {
            log.warn("Ollama describeImage failed: {}", exception.getMessage());
            return null;
        }
    }

    private String downloadAndEncodeBase64(String imageUrl) {
        try {
            byte[] imageBytes = RestClient.create().get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);
            if (imageBytes == null || imageBytes.length == 0) {
                return null;
            }
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.warn("Failed to download image from URL: {}, error: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra Ollama có đang chạy không. Trả về "available" / "unavailable".
     */
    public String getStatus() {
        if (!properties.enabled()) {
            return "disabled";
        }
        try {
            restClient.get()
                .uri("/api/tags")
                .retrieve()
                .toBodilessEntity();
            return "available";
        } catch (RuntimeException exception) {
            log.warn("Ollama health check failed: {}", exception.getMessage());
            return "unavailable";
        }
    }
}
