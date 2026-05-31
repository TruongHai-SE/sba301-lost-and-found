package com.sba301.lostandfound.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lostAndFoundOpenApi() {
        return new OpenAPI().info(
            new Info()
                .title("SBA301 Lost and Found Backend API")
                .version("0.0.1")
                .description("Backend API for the Lost and Found system")
        );
    }
}
