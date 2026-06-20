package com.sba301.lostandfound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for setting up a local password (Google-only users)")
public class SetupPasswordRequest {

    @NotBlank(message = "New password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain uppercase, lowercase, digit and special character"
    )
    @Schema(description = "New password to set", example = "StrongPassword@123")
    private String newPassword;

    @NotBlank(message = "Confirm password must not be blank")
    @Schema(description = "Must match newPassword exactly", example = "StrongPassword@123")
    private String confirmPassword;
}
