package com.sba301.lostandfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sba301.lostandfound.dto.AdminUpdateUserRequest;
import com.sba301.lostandfound.dto.ChangeUserRoleRequest;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.UserResponse;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.UserType;
import com.sba301.lostandfound.repository.MatchRequestRepository;
import com.sba301.lostandfound.repository.RefreshTokenRepository;
import com.sba301.lostandfound.repository.UserRepository;
import com.sba301.lostandfound.service.impl.UserServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MatchRequestRepository matchRequestRepository;

    private UserServiceImpl userService;

    // --- Helpers ---------------------------------------------------------

    /**
     * Build a User and inject the given id via reflection.
     * User has no public setId — id is normally set by JPA on persist.
     */
    private User buildUser(Long id, String name, String mail, String phone, UserType type) {
        User user = User.builder()
                .name(name)
                .mail(mail)
                .password("hashed-password")
                .phone(phone)
                .type(type)
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Test setup failed: cannot set User.id", e);
        }
        return user;
    }

    private User admin(Long id) {
        return buildUser(id, "Admin", "admin@test.com", null, UserType.ADMIN);
    }

    private User regularUser(Long id) {
        return buildUser(id, "User", "user@test.com", "0901234567", UserType.USER);
    }

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, refreshTokenRepository, matchRequestRepository);
    }

    // =========================================================================
    // getUsers — validation
    // =========================================================================

    @Test
    void getUsers_validParams_returnsPage() {
        User u = regularUser(1L);
        given(userRepository.findBySearchAndRole(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(u)));

        PageResponse<UserResponse> result = userService.getUsers(0, 10, null, null, "id", "asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMail()).isEqualTo("user@test.com");
    }

    @Test
    void getUsers_invalidSortBy_throwsBadRequest() {
        assertThatThrownBy(() -> userService.getUsers(0, 10, null, null, "password", "asc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getUsers_sizeExceedsMax_throwsBadRequest() {
        assertThatThrownBy(() -> userService.getUsers(0, 51, null, null, "id", "asc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getUsers_sizeZero_throwsBadRequest() {
        assertThatThrownBy(() -> userService.getUsers(0, 0, null, null, "id", "asc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getUsers_negativePageIndex_throwsBadRequest() {
        assertThatThrownBy(() -> userService.getUsers(-1, 10, null, null, "id", "asc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getUsers_invalidSortDir_throwsBadRequest() {
        assertThatThrownBy(() -> userService.getUsers(0, 10, null, null, "id", "invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getUsers_blankSearch_treatedAsNoSearch() {
        User u = regularUser(1L);
        given(userRepository.findBySearchAndRole(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(u)));

        PageResponse<UserResponse> result = userService.getUsers(0, 10, "   ", null, "id", "asc");
        assertThat(result.getContent()).hasSize(1);
    }

    // =========================================================================
    // getUserById
    // =========================================================================

    @Test
    void getUserById_exists_returnsResponse() {
        User u = regularUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(u));

        UserResponse result = userService.getUserById(1L);

        assertThat(result.getMail()).isEqualTo("user@test.com");
        assertThat(result.getType()).isEqualTo(UserType.USER);
    }

    @Test
    void getUserById_notFound_throwsNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getUserById_doesNotExposePassword() {
        User u = regularUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(u));

        UserResponse result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().equalsIgnoreCase("password"));
    }

    // =========================================================================
    // updateUser
    // =========================================================================

    @Test
    void updateUser_validData_updatesNameAndPhone() {
        User u = regularUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(userRepository.existsByPhoneAndIdNot("0911111111", 1L)).willReturn(false);
        given(userRepository.save(any())).willReturn(u);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setName("New Name");
        req.setPhone("0911111111");

        UserResponse result = userService.updateUser(1L, req);

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updateUser_phoneTakenByOther_throwsConflict() {
        User u = regularUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(userRepository.existsByPhoneAndIdNot("0911111111", 1L)).willReturn(true);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setPhone("0911111111");

        assertThatThrownBy(() -> userService.updateUser(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateUser_adminUpdatesOwnProfile_succeeds() {
        // Admin IS allowed to update their own name/phone — no self-update restriction
        User adminUser = admin(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
        given(userRepository.save(any())).willReturn(adminUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setName("New Admin Name");

        UserResponse result = userService.updateUser(1L, req);
        assertThat(result.getName()).isEqualTo("New Admin Name");
    }

    // =========================================================================
    // changeUserRole
    // =========================================================================

    @Test
    void changeUserRole_validChange_revokesTokens() {
        User currentAdmin = admin(1L);
        User target = regularUser(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRepository.save(any())).willReturn(target);

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.ADMIN);

        userService.changeUserRole(2L, req, currentAdmin);

        then(refreshTokenRepository).should().revokeAllByUserId(2L);
    }

    @Test
    void changeUserRole_selfChange_throwsForbidden() {
        User currentAdmin = admin(1L);

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.USER);

        assertThatThrownBy(() -> userService.changeUserRole(1L, req, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void changeUserRole_sameRole_doesNotRevokeTokens() {
        User currentAdmin = admin(1L);
        User target = regularUser(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.USER); // same as current

        userService.changeUserRole(2L, req, currentAdmin);

        then(refreshTokenRepository).should(never()).revokeAllByUserId(anyLong());
    }

    @Test
    void changeUserRole_demoteLastAdmin_throwsForbidden() {
        // Even though self-change is prevented above, another admin could demote the last admin
        User currentAdmin = admin(1L);
        User target = admin(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRepository.countByType(UserType.ADMIN)).willReturn(1L);

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.USER);

        assertThatThrownBy(() -> userService.changeUserRole(2L, req, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        then(userRepository).should(never()).save(any());
        then(refreshTokenRepository).should(never()).revokeAllByUserId(anyLong());
    }

    // =========================================================================
    // deleteUser
    // =========================================================================

    @Test
    void deleteUser_regularUser_noData_succeeds() {
        User currentAdmin = admin(1L);
        User target = regularUser(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRepository.hasAnyPost(2L)).willReturn(false);
        given(matchRequestRepository.existsByUserId(2L)).willReturn(false);

        userService.deleteUser(2L, currentAdmin);

        then(userRepository).should().delete(target);
    }

    @Test
    void deleteUser_selfDelete_throwsForbidden() {
        User currentAdmin = admin(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(currentAdmin));

        assertThatThrownBy(() -> userService.deleteUser(1L, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        then(userRepository).should(never()).delete(any());
    }

    @Test
    void deleteUser_lastAdmin_throwsForbidden() {
        User currentAdmin = admin(1L);
        User target = admin(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRepository.countByType(UserType.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(2L, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        then(userRepository).should(never()).delete(any());
    }

    @Test
    void deleteUser_hasPost_throwsConflict() {
        User currentAdmin = admin(1L);
        User target = regularUser(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRepository.hasAnyPost(2L)).willReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(2L, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        then(userRepository).should(never()).delete(any());
    }

    @Test
    void deleteUser_hasMatchRequest_throwsConflict() {
        User currentAdmin = admin(1L);
        User target = regularUser(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRepository.hasAnyPost(2L)).willReturn(false);
        given(matchRequestRepository.existsByUserId(2L)).willReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(2L, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        then(userRepository).should(never()).delete(any());
    }

    @Test
    void deleteUser_notFound_throwsNotFound() {
        User currentAdmin = admin(1L);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L, currentAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
