package com.sba301.lostandfound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for admin updating a user's profile")
public class AdminUpdateUserRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Full name of the user", example = "Nguyen Van B")
    private String name;

    @Pattern(
        regexp = "^$|^\\d{10}$",
        message = "Phone must be exactly 10 digits or empty"
    )
    @Schema(description = "Phone number (10 digits) or null to clear", example = "0912345678")
    private String phone;
}
