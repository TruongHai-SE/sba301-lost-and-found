package com.sba301.lostandfound.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lostAndFoundOpenApi() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
            .info(
                new Info()
                    .title("SBA301 Lost and Found Backend API")
                    .version("0.0.1")
                    .description("Backend API for the Lost and Found system")
            )
            .addSecurityItem(new SecurityRequirement().addList(schemeName))
            .components(
                new Components().addSecuritySchemes(schemeName,
                    new SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );
    }
}
