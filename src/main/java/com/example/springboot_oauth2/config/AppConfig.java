package com.example.springboot_oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /**
     * RestClient là HTTP client hiện đại của Spring (thay cho RestTemplate).
     * Ta dùng nó để gọi token endpoint + Google Calendar API.
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
