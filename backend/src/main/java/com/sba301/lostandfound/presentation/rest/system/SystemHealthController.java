package com.sba301.lostandfound.presentation.rest.system;

import com.sba301.lostandfound.application.port.in.GetSystemHealthUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    private final GetSystemHealthUseCase getSystemHealthUseCase;

    public SystemHealthController(GetSystemHealthUseCase getSystemHealthUseCase) {
        this.getSystemHealthUseCase = getSystemHealthUseCase;
    }

    @GetMapping("/health")
    public SystemHealthResponse health() {
        return SystemHealthResponse.from(getSystemHealthUseCase.getHealth());
    }
}
