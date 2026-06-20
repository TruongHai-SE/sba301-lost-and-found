package com.sba301.lostandfound.client;

import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipHealthApiResponse;
import com.sba301.lostandfound.dto.ClipSearchRequest;
import com.sba301.lostandfound.dto.EmbedImageRequest;
import com.sba301.lostandfound.dto.EmbedTextRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper gọi CLIP service (FastAPI).
 *
 * <p>Mapping endpoints:
 * <ul>
 *   <li>GET  /api/v1/health             → health</li>
 *   <li>POST /api/v1/embeddings/image   → embedImage</li>
 *   <li>POST /api/v1/embeddings/text    → embedText</li>
 *   <li>POST /api/v1/search             → search (cross-type)</li>
 *   <li>DEL  /api/v1/embeddings/posts/{id} → deleteEmbeddings (PostController)</li>
 * </ul>
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
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(new EmbedTextRequest(postId, text, true, postType))
            .retrieve()
            .body(ClipEmbedResponse.class);
    }

    public ClipEmbedResponse embedImage(Long postId, String imageUrl, Long imageId, String postType) {
        return restClient.post()
            .uri("/api/v1/embeddings/image")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(new EmbedImageRequest(postId, imageUrl, imageId, postType))
            .retrieve()
            .body(ClipEmbedResponse.class);
    }

    /**
     * Gọi CLIP search. Trả về ClipEmbedResponse (matches: List<ClipMatch>).
     * CLIP service tự xử lý cross-type matching (LOST ↔ FOUND).
     */
    public ClipEmbedResponse search(String queryImageUrl, String queryText,
                                    String targetPostType, int topK) {
        ClipSearchRequest body = ClipSearchRequest.of(queryText, queryImageUrl, targetPostType, topK);
        return restClient.post()
            .uri("/api/v1/search")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(ClipEmbedResponse.class);
    }
}
