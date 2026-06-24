package com.sba301.lostandfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.lostandfound.dto.AdminUpdateUserRequest;
import com.sba301.lostandfound.dto.ChangeUserRoleRequest;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.UserResponse;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.UserType;
import com.sba301.lostandfound.security.CustomUserDetails;
import com.sba301.lostandfound.security.JwtAuthenticationFilter;
import com.sba301.lostandfound.security.JwtTokenProvider;
import com.sba301.lostandfound.service.UserService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = AdminUserController.class)
@Import(com.sba301.lostandfound.config.SecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
            return new JwtAuthenticationFilter(jwtTokenProvider, null) {
                @Override
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                jakarta.servlet.FilterChain filterChain)
                        throws jakarta.servlet.ServletException, java.io.IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private CustomUserDetails adminPrincipal() {
        User adminUser = User.builder()
                .name("Admin")
                .mail("admin@test.com")
                .password("hash")
                .type(UserType.ADMIN)
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(adminUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new CustomUserDetails(adminUser);
    }

    private UserResponse sampleUserResponse() {
        return UserResponse.builder()
                .id(2L)
                .name("Test User")
                .mail("user@test.com")
                .phone("0901234567")
                .type(UserType.USER)
                .createAt(LocalDate.of(2024, 1, 1))
                .googleAccount(false)
                .build();
    }

    // -----------------------------------------------------------------------
    // Authorization — No token → 401
    // -----------------------------------------------------------------------

    @Test
    void listUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getUserById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/admin/users — list
    // -----------------------------------------------------------------------

    @Test
    void listUsers_validAdminRequest_returns200() throws Exception {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .content(List.of(sampleUserResponse()))
                .pageNumber(0).pageSize(10).totalElements(1).totalPages(1).isLast(true)
                .build();
        when(userService.getUsers(anyInt(), anyInt(), any(), any(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].mail").value("user@test.com"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist());
    }

    @Test
    void listUsers_invalidSortBy_returns400() throws Exception {
        when(userService.getUsers(anyInt(), anyInt(), any(), any(), eq("password"), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("sortBy", "password")
                        .with(user(adminPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listUsers_invalidSortDir_returns400() throws Exception {
        when(userService.getUsers(anyInt(), anyInt(), any(), any(), anyString(), eq("random")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortDir must be 'asc' or 'desc'"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("sortDir", "random")
                        .with(user(adminPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listUsers_sizeExceedsMax_returns400() throws Exception {
        when(userService.getUsers(anyInt(), eq(51), any(), any(), anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 50"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("size", "51")
                        .with(user(adminPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listUsers_negativePageIndex_returns400() throws Exception {
        when(userService.getUsers(eq(-1), anyInt(), any(), any(), anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "-1")
                        .with(user(adminPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/admin/users/{id} — get by ID
    // -----------------------------------------------------------------------

    @Test
    void getUserById_exists_returns200WithNoSensitiveFields() throws Exception {
        when(userService.getUserById(2L)).thenReturn(sampleUserResponse());

        mockMvc.perform(get("/api/v1/admin/users/2")
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.mail").value("user@test.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.type").value("USER"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: 99"));

        mockMvc.perform(get("/api/v1/admin/users/99")
                        .with(user(adminPrincipal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/admin/users/{id} — update
    // -----------------------------------------------------------------------

    @Test
    void updateUser_validRequest_returns200() throws Exception {
        when(userService.updateUser(eq(2L), any(AdminUpdateUserRequest.class)))
                .thenReturn(sampleUserResponse());

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setName("Updated Name");
        req.setPhone("0912345678");

        mockMvc.perform(put("/api/v1/admin/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void updateUser_invalidPhoneFormat_returns400() throws Exception {
        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setPhone("12345"); // too short

        mockMvc.perform(put("/api/v1/admin/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void updateUser_phoneConflict_returns409() throws Exception {
        when(userService.updateUser(eq(2L), any(AdminUpdateUserRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use"));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setPhone("0912345678");

        mockMvc.perform(put("/api/v1/admin/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        when(userService.updateUser(eq(99L), any(AdminUpdateUserRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: 99"));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setName("Some Name");

        mockMvc.perform(put("/api/v1/admin/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/v1/admin/users/{id}/role — change role
    // -----------------------------------------------------------------------

    @Test
    void changeRole_validRequest_returns200() throws Exception {
        when(userService.changeUserRole(eq(2L), any(ChangeUserRoleRequest.class), any(User.class)))
                .thenReturn(sampleUserResponse());

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.ADMIN);

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void changeRole_selfChange_returns403() throws Exception {
        when(userService.changeUserRole(eq(1L), any(ChangeUserRoleRequest.class), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot change their own role"));

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.USER);

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void changeRole_nullRole_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newRole\": null}")
                        .with(user(adminPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void changeRole_lastAdmin_returns403() throws Exception {
        when(userService.changeUserRole(eq(2L), any(ChangeUserRoleRequest.class), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot demote the last admin account"));

        ChangeUserRoleRequest req = new ChangeUserRoleRequest();
        req.setNewRole(UserType.USER);

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(adminPrincipal())))
                .andExpect(status().isForbidden());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/admin/users/{id} — delete
    // -----------------------------------------------------------------------

    @Test
    void deleteUser_validRequest_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    void deleteUser_selfDelete_returns403() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot delete their own account"))
                .when(userService).deleteUser(eq(1L), any(User.class));

        mockMvc.perform(delete("/api/v1/admin/users/1")
                        .with(user(adminPrincipal())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteUser_lastAdmin_returns403() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete the last admin account"))
                .when(userService).deleteUser(eq(2L), any(User.class));

        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .with(user(adminPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_hasPost_returns409() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete user: they still have posts."))
                .when(userService).deleteUser(eq(2L), any(User.class));

        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .with(user(adminPrincipal())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteUser_hasMatchRequest_returns409() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete user: they still have match requests."))
                .when(userService).deleteUser(eq(2L), any(User.class));

        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .with(user(adminPrincipal())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: 99"))
                .when(userService).deleteUser(eq(99L), any(User.class));

        mockMvc.perform(delete("/api/v1/admin/users/99")
                        .with(user(adminPrincipal())))
                .andExpect(status().isNotFound());
    }
}
