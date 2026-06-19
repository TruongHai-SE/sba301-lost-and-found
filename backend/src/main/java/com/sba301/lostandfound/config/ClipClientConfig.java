package com.sba301.lostandfound.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class ClipClientConfig {

    @Bean
    RestClient clipRestClient(ClipClientProperties properties) {
        ObjectMapper objectMapper = new ObjectMapper();
        
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
            
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
            .requestFactory(requestFactory)
            .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter(objectMapper)))
            .baseUrl(properties.baseUrl().toString())
            .build();
    }
}
