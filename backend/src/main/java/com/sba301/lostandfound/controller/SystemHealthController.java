package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.SystemHealthResponse;
import com.sba301.lostandfound.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> health() {
        SystemHealthResponse response = SystemHealthResponse.from(systemHealthService.getHealth());
        return ResponseEntity.ok(ApiResponse.success(response, "System health retrieved successfully"));
    }
}
