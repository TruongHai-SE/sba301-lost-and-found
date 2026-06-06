package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.SystemHealthResponse;
import com.sba301.lostandfound.service.SystemHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    public SystemHealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/health")
    public SystemHealthResponse health() {
        return SystemHealthResponse.from(systemHealthService.getHealth());
    }
}
