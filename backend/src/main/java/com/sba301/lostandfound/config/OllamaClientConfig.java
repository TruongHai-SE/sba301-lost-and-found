package com.sba301.lostandfound.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Tạo RestClient riêng cho Ollama với timeout riêng (Qwen-VL mất 5-30s/ảnh).
 * Tách khỏi clipRestClient vì cấu hình HTTP khác nhau.
 *
 * Đồng thời expose ObjectMapper làm Spring bean dùng chung cho toàn app:
 * ImageAnalysisServiceImpl và PostAiEnrichmentService cần inject nó qua constructor.
 */
@Configuration
public class OllamaClientConfig {

    /**
     * Shared ObjectMapper bean — dùng chung để serialize/deserialize JSON.
     * Tránh tạo nhiều instance riêng lẻ và cho phép Spring inject đúng nơi cần.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    RestClient ollamaRestClient(OllamaProperties properties, ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Timeout riêng cho Ollama (model vision chậm hơn nhiều so với CLIP)
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        return RestClient.builder()
            .requestFactory(requestFactory)
            .messageConverters(converters ->
                converters.add(new MappingJackson2HttpMessageConverter(objectMapper)))
            .baseUrl(properties.baseUrl().toString())
            .build();
    }
}
