package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "User profile response (safe — no password or tokens)")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private final Long id;

    @Schema(description = "Full name", example = "Nguyen Van A")
    private final String name;

    @Schema(description = "Email address", example = "user@example.com")
    private final String mail;

    @Schema(description = "Phone number", example = "0901234567")
    private final String phone;

    @Schema(description = "Social link or avatar URL")
    private final String socialLink;

    @Schema(description = "User role", example = "USER")
    private final UserType type;

    @Schema(description = "Account creation date", example = "2024-01-15")
    private final LocalDate createdAt;

    @Schema(description = "Whether the user registered via Google (no password set)", example = "false")
    private final boolean googleAccount;

    @Schema(description = "Whether the user has a local password set", example = "true")
    private final boolean hasPassword;
}
