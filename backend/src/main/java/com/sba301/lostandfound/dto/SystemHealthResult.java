package com.sba301.lostandfound.dto;

import java.time.Instant;

public record SystemHealthResult(
    String backend,
    String database,
    String clip,
    String ollama,
    Instant checkedAt
) {
}
