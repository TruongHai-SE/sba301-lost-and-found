package com.sba301.lostandfound.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cho Ollama server. Tất cả field có default để dev không cần set env cũng chạy được.
 *
 * - baseUrl:        URL của Ollama REST API (mặc định http://localhost:11434)
 * - visionModel:    tên model vision-language (qwen2.5vl, llama3.2-vision, llava, ...)
 * - timeoutSeconds: timeout cho mỗi request (Qwen-VL chậm, mặc định 60s)
 * - enabled:        bật/tắt toàn bộ flow AI enrichment
 */
@ConfigurationProperties(prefix = "services.ollama")
public record OllamaProperties(
    URI baseUrl,
    String visionModel,
    int timeoutSeconds,
    boolean enabled
) {
}
