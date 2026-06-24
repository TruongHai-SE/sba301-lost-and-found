package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.AdminUpdateUserRequest;
import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.ChangeUserRoleRequest;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.UserResponse;
import com.sba301.lostandfound.entity.enums.UserType;
import com.sba301.lostandfound.security.CustomUserDetails;
import com.sba301.lostandfound.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — User Management", description = "Endpoints for Admin to manage users. Requires ADMIN role.")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(
        summary = "List users",
        description = "Returns a paginated, searchable, filterable, and sortable list of all users. "
                    + "Allowed sortBy values: id, name, mail, createdAt. Max page size: 50."
    )
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @Parameter(description = "0-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (max 50)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search by name or email (case-insensitive)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by role")
            @RequestParam(required = false) UserType role,
            @Parameter(description = "Sort field: id | name | mail | createdAt", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc | desc", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<UserResponse> response = userService.getUsers(page, size, search, role, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response, "Users retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Returns full profile of a single user. Does not expose password or tokens."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user profile",
        description = "Allows admin to update a user's name and phone. "
                    + "Email and password cannot be changed through this endpoint."
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }

    @PatchMapping("/{id}/role")
    @Operation(
        summary = "Change user role",
        description = "Changes a user's role between USER and ADMIN. "
                    + "Admins cannot change their own role. "
                    + "All refresh tokens of the affected user are revoked immediately."
    )
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUserDetails
    ) {
        UserResponse response = userService.changeUserRole(id, request, currentUserDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success(response, "User role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user",
        description = "Permanently deletes a user account. "
                    + "Will fail if: the user is the last admin (403), or the user still has posts (409)."
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUserDetails
    ) {
        userService.deleteUser(id, currentUserDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
