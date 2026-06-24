package com.sba301.lostandfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.lostandfound.dto.AuthResponse;
import com.sba301.lostandfound.dto.RegisterRequest;
import com.sba301.lostandfound.security.JwtAuthenticationFilter;
import com.sba301.lostandfound.security.JwtTokenProvider;
import com.sba301.lostandfound.service.AuthService;
import com.sba301.lostandfound.service.ImageAnalysisService;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.PostService;
import com.sba301.lostandfound.service.SystemHealthService;
import com.sba301.lostandfound.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = {AuthController.class, SystemHealthController.class, PostController.class})
@org.springframework.context.annotation.Import(com.sba301.lostandfound.config.SecurityConfig.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private com.sba301.lostandfound.service.ImageStorageService imageStorageService;

    @MockitoBean
    private com.sba301.lostandfound.service.ImageAnalysisService imageAnalysisService;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private ImageAnalysisService imageAnalysisService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
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

        @org.springframework.context.annotation.Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }
    }

    @Test
    public void registerFailsWithValidationErrors() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setMail("invalid-email");
        request.setPassword("12"); // too short, misses complexity
        request.setName("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.mail").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void registerSuccessReturnsUnifiedResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setMail("test@example.com");
        request.setPassword("Password123!");
        request.setName("Test User");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .mail("test@example.com")
                .name("Test User")
                .userId(1L)
                .userType("USER")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.mail").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    public void responseStatusExceptionReturnsUnifiedResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setMail("conflict@example.com");
        request.setPassword("Password123!");
        request.setName("Conflict User");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void unexpectedExceptionReturnsUnifiedResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setMail("error@example.com");
        request.setPassword("Password123!");
        request.setName("Error User");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Database down"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    public void callingProtectedRouteWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized: Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/v1/posts"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
