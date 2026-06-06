package com.sba301.lostandfound.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.clip")
public record ClipClientProperties(URI baseUrl) {
}
