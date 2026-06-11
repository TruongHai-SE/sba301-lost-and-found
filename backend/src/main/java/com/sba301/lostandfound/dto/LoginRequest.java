package com.sba301.lostandfound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(example = "user@example.com")
    private String mail;

    @NotBlank
    @Schema(example = "MyPass@123")
    private String password;
}
