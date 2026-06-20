package com.sba301.lostandfound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
@Schema(description = "Generic paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "List of items on current page")
    private final List<T> content;

    @Schema(description = "Current page number (0-based)", example = "0")
    private final int page;

    @Schema(description = "Number of items per page", example = "10")
    private final int size;

    @Schema(description = "Total number of items across all pages", example = "42")
    private final long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private final int totalPages;

    @Schema(description = "Whether this is the last page", example = "false")
    private final boolean last;

    /**
     * Convenience factory — builds a PageResponse from a Spring Data {@link Page}.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
