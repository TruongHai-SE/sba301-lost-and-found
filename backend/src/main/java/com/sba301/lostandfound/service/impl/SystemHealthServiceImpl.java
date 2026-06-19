package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.client.DatabaseHealthClient;
import com.sba301.lostandfound.client.OllamaClient;
import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.SystemHealthResult;
import com.sba301.lostandfound.service.SystemHealthService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private final DatabaseHealthClient databaseHealthClient;
    private final ClipClient clipClient;
    private final OllamaClient ollamaClient;

    public SystemHealthServiceImpl(
        DatabaseHealthClient databaseHealthClient,
        ClipClient clipClient,
        OllamaClient ollamaClient
    ) {
        this.databaseHealthClient = databaseHealthClient;
        this.clipClient = clipClient;
        this.ollamaClient = ollamaClient;
    }

    @Override
    public SystemHealthResult getHealth() {
        databaseHealthClient.checkHealth();

        String clipStatus;
        try {
            clipStatus = clipClient.getHealthStatus();
        } catch (RuntimeException exception) {
            clipStatus = "unavailable";
        }

        String ollamaStatus;
        try {
            ollamaStatus = ollamaClient.getStatus();
        } catch (RuntimeException exception) {
            ollamaStatus = "unavailable";
        }

        return new SystemHealthResult("ok", "ok", clipStatus, ollamaStatus, Instant.now());
    }
}
