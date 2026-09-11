package com.poly.config;

import java.time.Duration;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiHttpClientConfig {

    @Bean
    OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofMinutes(2));

        return OllamaApi.builder()
            .baseUrl(baseUrl)
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .webClientBuilder(WebClient.builder())
            .build();
    }
}
