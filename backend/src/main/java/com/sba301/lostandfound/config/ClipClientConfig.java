package com.sba301.lostandfound.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClipClientConfig {

    @Bean
    RestClient clipRestClient(ClipClientProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.baseUrl().toString())
            .build();
    }
}
