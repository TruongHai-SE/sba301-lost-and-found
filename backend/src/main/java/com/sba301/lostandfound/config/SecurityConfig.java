package com.sba301.lostandfound.config;

import com.sba301.lostandfound.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // =========================================================================
    // SECURITY FILTER CHAIN
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Endpoints within the auth path that REQUIRE authentication
                .requestMatchers(
                    "/api/v1/auth/password/setup",
                    "/api/v1/auth/me"
                ).authenticated()

                // 2. Public Auth & Identity Endpoints
                .requestMatchers(
                    "/api/v1/auth/**"
                ).permitAll()

                // 3. System & Monitoring Endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api/v1/system/**"
                ).permitAll()

                // 4. API Documentation (Swagger/OpenAPI)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // 5. Static Assets (Web Client & UI testing)
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/*.css",
                    "/*.js",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // 6. Admin Panel Endpoints (Requires ADMIN role)
                .requestMatchers(
                    "/api/v1/admin/**"
                ).hasRole("ADMIN")

                // 7. All other application requests (Requires login: USER or ADMIN role)
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // EXCEPTION HANDLERS (401 & 403 response wrappers)
    // =========================================================================

    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            
            com.sba301.lostandfound.dto.ApiResponse<Void> apiResponse = com.sba301.lostandfound.dto.ApiResponse.error(
                    jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: " + authException.getMessage(),
                    request.getRequestURI(),
                    null
            );
            
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
        };
    }

    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
            
            com.sba301.lostandfound.dto.ApiResponse<Void> apiResponse = com.sba301.lostandfound.dto.ApiResponse.error(
                    jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN,
                    "Access denied: " + accessDeniedException.getMessage(),
                    request.getRequestURI(),
                    null
            );
            
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
        };
    }

    // =========================================================================
    // BEANS & UTILITIES
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        if ("*".equals(allowedOrigins)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(true);
        } else {
            configuration.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .toList());
            configuration.setAllowCredentials(true);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
