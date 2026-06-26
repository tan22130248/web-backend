package com.fashion.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ImageSearchConfig {

    @Value("${app.image-search.url:http://localhost:8000}")
    private String imageSearchUrl;

    @Value("${app.image-search.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${app.image-search.read-timeout-ms:15000}")
    private int readTimeoutMs;

    @Bean
    public RestClient imageSearchRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .baseUrl(imageSearchUrl)
                .requestFactory(factory)
                .build();
    }
}
