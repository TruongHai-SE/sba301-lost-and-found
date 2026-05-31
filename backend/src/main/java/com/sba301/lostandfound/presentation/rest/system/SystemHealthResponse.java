package com.sba301.lostandfound.presentation.rest.system;

import com.sba301.lostandfound.application.result.SystemHealthResult;
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
