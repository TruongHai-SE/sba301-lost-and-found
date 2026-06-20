package com.sba301.lostandfound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Response after successfully setting up a local password")
public class SetupPasswordResponse {

    @Schema(description = "Whether the user now has a local password set", example = "true")
    private final boolean hasPassword;

    @Schema(description = "Informational message", example = "Password set successfully. You can now log in with email and password.")
    private final String message;
}
