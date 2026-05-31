package com.sba301.lostandfound.application.result;

import java.time.Instant;

public record SystemHealthResult(
    String backend,
    String database,
    String clip,
    Instant checkedAt
) {
}
