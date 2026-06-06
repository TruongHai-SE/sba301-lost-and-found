package com.sba301.lostandfound.dto;

import java.time.Instant;

public record SystemHealthResponse(
    String backend,
    String database,
    String clip,
    Instant checkedAt
) {
    public static SystemHealthResponse from(SystemHealthResult result) {
        return new SystemHealthResponse(
            result.backend(),
            result.database(),
            result.clip(),
            result.checkedAt()
        );
    }
}
