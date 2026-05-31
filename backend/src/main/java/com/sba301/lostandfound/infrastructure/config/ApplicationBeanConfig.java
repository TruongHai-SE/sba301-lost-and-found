package com.sba301.lostandfound.infrastructure.config;

import com.sba301.lostandfound.application.port.in.GetSystemHealthUseCase;
import com.sba301.lostandfound.application.port.out.CheckDatabaseHealthPort;
import com.sba301.lostandfound.application.port.out.GetClipHealthPort;
import com.sba301.lostandfound.application.service.SystemHealthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeanConfig {

    @Bean
    GetSystemHealthUseCase getSystemHealthUseCase(
        CheckDatabaseHealthPort databaseHealthPort,
        GetClipHealthPort clipHealthPort
    ) {
        return new SystemHealthService(databaseHealthPort, clipHealthPort);
    }
}
