package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.AdminUpdateUserRequest;
import com.sba301.lostandfound.dto.ChangeUserRoleRequest;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.UserResponse;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.UserType;

public interface UserService {

    /**
     * Returns a paginated, filtered and sorted list of users.
     *
     * @param page    0-based page index
     * @param size    number of results per page (max 50)
     * @param search  optional substring to match against name or mail (case-insensitive)
     * @param role    optional filter by UserType
     * @param sortBy  field to sort on — whitelisted: id, name, mail, createAt
     * @param sortDir "asc" or "desc"
     */
    PageResponse<UserResponse> getUsers(int page, int size, String search, UserType role,
                                         String sortBy, String sortDir);

    /**
     * Returns a single user by ID, or throws 404.
     */
    UserResponse getUserById(Long id);

    /**
     * Updates permitted profile fields (name, phone).
     * Throws 404 if not found, 409 if phone is already taken by another user.
     */
    UserResponse updateUser(Long id, AdminUpdateUserRequest request);

    /**
     * Changes the user's role.
     * Throws 403 if the admin is trying to change their own role.
     * Revokes all refresh tokens for the affected user after a role change.
     */
    UserResponse changeUserRole(Long id, ChangeUserRoleRequest request, User currentAdmin);

    /**
     * Deletes a user account.
     * Throws 403 if target is the last remaining ADMIN.
     * Throws 409 if the user still has posts (FK safety).
     */
    void deleteUser(Long id, User currentAdmin);
}
