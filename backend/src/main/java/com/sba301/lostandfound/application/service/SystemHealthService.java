package com.sba301.lostandfound.application.service;

import com.sba301.lostandfound.application.port.in.GetSystemHealthUseCase;
import com.sba301.lostandfound.application.port.out.CheckDatabaseHealthPort;
import com.sba301.lostandfound.application.port.out.GetClipHealthPort;
import com.sba301.lostandfound.application.result.SystemHealthResult;
import java.time.Instant;

public class SystemHealthService implements GetSystemHealthUseCase {

    private final CheckDatabaseHealthPort databaseHealthPort;
    private final GetClipHealthPort clipHealthPort;

    public SystemHealthService(
        CheckDatabaseHealthPort databaseHealthPort,
        GetClipHealthPort clipHealthPort
    ) {
        this.databaseHealthPort = databaseHealthPort;
        this.clipHealthPort = clipHealthPort;
    }

    @Override
    public SystemHealthResult getHealth() {
        databaseHealthPort.checkHealth();

        String clipStatus;
        try {
            clipStatus = clipHealthPort.getHealthStatus();
        } catch (RuntimeException exception) {
            clipStatus = "unavailable";
        }

        return new SystemHealthResult("ok", "ok", clipStatus, Instant.now());
    }
}
