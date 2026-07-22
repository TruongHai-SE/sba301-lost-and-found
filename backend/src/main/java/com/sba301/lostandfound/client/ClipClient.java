package com.sba301.lostandfound.client;

import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipHealthApiResponse;
import com.sba301.lostandfound.dto.ClipSearchRequest;
import com.sba301.lostandfound.dto.EmbedImageRequest;
import com.sba301.lostandfound.dto.EmbedTextRequest;
import com.sba301.lostandfound.dto.ImageValidationResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * Thin wrapper gọi CLIP service (FastAPI).
 */
@Component
public class ClipClient {

    private final RestClient restClient;

    public ClipClient(RestClient clipRestClient) {
        this.restClient = clipRestClient;
    }

    public String getHealthStatus() {
        ClipHealthApiResponse response = restClient.get()
            .uri("/api/v1/health")
            .retrieve()
            .body(ClipHealthApiResponse.class);

        return response == null ? "unavailable" : response.status();
    }

    public ClipEmbedResponse embedText(Long postId, String text, String postType) {
        return restClient.post()
            .uri("/api/v1/embeddings/text")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new EmbedTextRequest(postId, text, true, postType))
            .retrieve()
            .body(ClipEmbedResponse.class);
    }

    public ClipEmbedResponse embedImage(Long postId, String imageUrl, Long imageId, String postType) {
        return restClient.post()
            .uri("/api/v1/embeddings/image")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new EmbedImageRequest(postId, imageUrl, imageId, postType))
            .retrieve()
            .body(ClipEmbedResponse.class);
    }

    public ClipEmbedResponse search(String queryImageUrl, String queryText,
                                    String targetPostType, int topK) {
        ClipSearchRequest body = ClipSearchRequest.of(queryText, queryImageUrl, targetPostType, topK);
        return restClient.post()
            .uri("/api/v1/search")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(ClipEmbedResponse.class);
    }

    public ImageValidationResponse validateImage(MultipartFile image, String imageUrl, String title) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (image != null && !image.isEmpty()) {
                body.add("image", image.getResource());
            }
            if (imageUrl != null && !imageUrl.isBlank()) {
                body.add("image_url", imageUrl);
            }
            if (title != null && !title.isBlank()) {
                body.add("title", title);
            }

            return restClient.post()
                .uri("/api/v1/validate-image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ImageValidationResponse.class);
        } catch (Exception e) {
            // Fallback gracefully if validation endpoint fails or times out
            return new ImageValidationResponse(true, "OK", "Validation bypassed");
        }
    }

    public ClipEmbedResponse searchByImageBytes(MultipartFile image, String targetPostType, int topK) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", image.getResource());
        body.add("target_post_type", targetPostType != null ? targetPostType : "ALL");
        body.add("top_k", topK);

        return restClient.post()
            .uri("/api/v1/search/image-bytes")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(ClipEmbedResponse.class);
    }
}
