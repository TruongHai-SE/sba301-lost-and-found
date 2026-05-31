package com.sba301.lostandfound.infrastructure.integration.clip;

import com.sba301.lostandfound.application.port.out.GetClipHealthPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClipHealthAdapter implements GetClipHealthPort {

    private final RestClient restClient;

    public ClipHealthAdapter(RestClient clipRestClient) {
        this.restClient = clipRestClient;
    }

    @Override
    public String getHealthStatus() {
        ClipHealthApiResponse response = restClient.get()
            .uri("/api/clip/health")
            .retrieve()
            .body(ClipHealthApiResponse.class);

        return response == null ? "unavailable" : response.status();
    }
}
