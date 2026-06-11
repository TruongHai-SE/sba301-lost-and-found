package com.sba301.lostandfound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Schema(example = "Nguyen Van A")
    private String name;

    @NotBlank
    @Email
    @Schema(example = "user@example.com")
    private String mail;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain uppercase, lowercase, digit and special character"
    )
    @Schema(example = "MyPass@123")
    private String password;

    @Size(max = 10)
    @Schema(example = "0901234567")
    private String phone;
}
