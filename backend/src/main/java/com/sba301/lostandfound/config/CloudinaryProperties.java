package com.sba301.lostandfound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {
}
