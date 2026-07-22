package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImageValidationResponse(
        @JsonProperty("is_valid") boolean isValid,
        @JsonProperty("reason_code") String reasonCode,
        @JsonProperty("message") String message
) {}
