package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for changing a user's role")
public class ChangeUserRoleRequest {

    @NotNull(message = "New role must not be null")
    @Schema(description = "The new role to assign", example = "ADMIN", allowableValues = {"USER", "ADMIN"})
    private UserType newRole;
}
