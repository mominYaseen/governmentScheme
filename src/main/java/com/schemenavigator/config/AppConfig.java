package com.schemenavigator.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.base-url}")
    private String geminiApiBaseUrl;

    @Value("${gemini.model}")
    private String geminiModel;

    @Value("${gemini.max-output-tokens:1000}")
    private int maxOutputTokensDefault;

    @Value("${gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${gemini.retry.max-attempts:3}")
    private int geminiRetryMaxAttempts;

    @Value("${gemini.retry.initial-delay-ms:2000}")
    private int geminiRetryInitialDelayMs;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getGeminiApiBaseUrl() {
        return geminiApiBaseUrl;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public int getMaxOutputTokensDefault() {
        return maxOutputTokensDefault;
    }

    public int getGeminiRetryMaxAttempts() {
        return Math.max(1, geminiRetryMaxAttempts);
    }

    public int getGeminiRetryInitialDelayMs() {
        return Math.max(100, geminiRetryInitialDelayMs);
    }
}
