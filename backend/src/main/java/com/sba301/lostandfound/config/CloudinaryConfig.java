package com.sba301.lostandfound.config;

import com.cloudinary.Cloudinary;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(Map.of(
            "cloud_name", properties.cloudName(),
            "api_key", properties.apiKey(),
            "api_secret", properties.apiSecret(),
            "secure", true
        ));
    }
}
