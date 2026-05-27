package com.fashion.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GhnConfig {

    @Value("${app.ghn.api-url}")
    private String ghnApiUrl;

    @Bean
    public RestClient ghnRestClient() {
        return RestClient.builder()
                .baseUrl(ghnApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
