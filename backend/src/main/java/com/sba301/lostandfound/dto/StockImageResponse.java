package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sba301.lostandfound.entity.StockImage;
import java.time.LocalDateTime;

public record StockImageResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("category") String category,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("label") String label,
        @JsonProperty("created_at") LocalDateTime createdAt) {

    public static StockImageResponse from(StockImage entity) {
        return new StockImageResponse(
                entity.getId(),
                entity.getCategory() != null ? entity.getCategory().name() : null,
                entity.getImageUrl(),
                entity.getLabel(),
                entity.getCreatedAt());
    }
}
