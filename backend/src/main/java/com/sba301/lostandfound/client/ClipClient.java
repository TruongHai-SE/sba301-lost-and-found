package com.sba301.lostandfound.client;

import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipHealthApiResponse;
import com.sba301.lostandfound.dto.EmbedImageRequest;
import com.sba301.lostandfound.dto.EmbedTextRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
            .body(new EmbedTextRequest(postId, text, true, postType))
            .retrieve()
            .body(ClipEmbedResponse.class);
    }

    public ClipEmbedResponse embedImage(Long postId, String imageUrl, Long imageId, String postType) {
        return restClient.post()
            .uri("/api/v1/embeddings/image")
            .body(new EmbedImageRequest(postId, imageUrl, imageId, postType))
            .retrieve()
            .body(ClipEmbedResponse.class);
    }
}
