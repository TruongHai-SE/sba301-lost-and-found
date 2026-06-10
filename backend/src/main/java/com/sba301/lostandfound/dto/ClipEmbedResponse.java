package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ClipEmbedResponse(
    @JsonProperty("embedding_id") Long embeddingId,
    @JsonProperty("dimension") Integer dimension,
    @JsonProperty("matches") List<ClipMatch> matches
) {
}
