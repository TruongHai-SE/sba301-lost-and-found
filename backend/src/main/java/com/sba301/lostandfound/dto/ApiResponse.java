package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified API Response Wrapper")
public class ApiResponse<T> {
    
    @Schema(description = "HTTP status code", example = "200")
    private final int status;

    @Schema(description = "Response message", example = "Operation successful")
    private final String message;

    @Schema(description = "Data payload returned on success (null on errors)")
    private final T data;

    @Schema(description = "Validation errors map (null if no validation errors occurred)")
    private final Map<String, String> errors;

    @Schema(description = "Request URL path (only returned on errors)", example = "/api/v1/auth/register")
    private final String path;

    @Schema(description = "Timestamp of the response")
    private final Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(int status, T data, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiResponse<Void> error(int status, String message, String path, Map<String, String> errors) {
        return ApiResponse.<Void>builder()
                .status(status)
                .message(message)
                .path(path)
                .errors(errors)
                .timestamp(Instant.now())
                .build();
    }
}
