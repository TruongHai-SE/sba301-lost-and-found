package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.service.SystemHealthService;

import com.sba301.lostandfound.client.DatabaseHealthClient;
import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.SystemHealthResult;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private final DatabaseHealthClient databaseHealthClient;
    private final ClipClient clipClient;

    public SystemHealthServiceImpl(
        DatabaseHealthClient databaseHealthClient,
        ClipClient clipClient
    ) {
        this.databaseHealthClient = databaseHealthClient;
        this.clipClient = clipClient;
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

        return new SystemHealthResult("ok", "ok", clipStatus, Instant.now());
    }
}
