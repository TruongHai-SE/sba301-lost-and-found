package com.sba301.lostandfound.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.sba301.lostandfound.dto.ClipHealthApiResponse;

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
}
