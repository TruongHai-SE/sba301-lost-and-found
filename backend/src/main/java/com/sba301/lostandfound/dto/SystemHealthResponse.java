package com.sba301.lostandfound.dto;

import java.time.Instant;

public record SystemHealthResponse(
    String backend,
    String database,
    String clip,
    String ollama,
    Instant checkedAt
) {
    public static SystemHealthResponse from(SystemHealthResult result) {
        return new SystemHealthResponse(
            result.backend(),
            result.database(),
            result.clip(),
            result.ollama(),
            result.checkedAt()
        );
    }
}
