package com.sba301.lostandfound.config;

import com.sba301.lostandfound.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed for request [{}]: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : "Error";
        
        if (ex.getStatusCode().is5xxServerError()) {
            log.error("Server error [{}] on path [{}]: {}", status, request.getRequestURI(), message, ex);
        } else {
            log.warn("Client error [{}] on path [{}]: {}", status, request.getRequestURI(), message);
        }

        ApiResponse<Void> body = ApiResponse.error(
                status,
                message,
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON request body on path [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request body",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HTTP method [{}] not supported on path [{}]: {}", ex.getMethod(), request.getRequestURI(), ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Request method '" + ex.getMethod() + "' is not supported",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No resource found on path [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on path [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed on path [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized: " + ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred on path [{}]", request.getRequestURI(), ex);
        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
