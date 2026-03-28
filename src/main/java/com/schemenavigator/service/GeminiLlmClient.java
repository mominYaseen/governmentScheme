package com.schemenavigator.service;

import com.schemenavigator.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls Google AI Studio / Gemini REST API ({@code :generateContent}).
 */
@Service
@Slf4j
public class GeminiLlmClient {

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;

    public GeminiLlmClient(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
    }

    /**
     * @param jsonResponse when true, sets {@code responseMimeType} to application/json (structured output).
     */
    @SuppressWarnings("unchecked")
    public String generate(String systemPrompt, String userMessage, int maxOutputTokens, boolean jsonResponse) {
        String base = appConfig.getGeminiApiBaseUrl().replaceAll("/$", "");
        String model = appConfig.getGeminiModel();
        String url = UriComponentsBuilder
                .fromHttpUrl(base + "/models/" + model + ":generateContent")
                .queryParam("key", appConfig.getGeminiApiKey())
                .build()
                .toUriString();

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );

        Map<String, Object> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("parts", List.of(Map.of("text", userMessage)));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        if (jsonResponse) {
            generationConfig.put("responseMimeType", "application/json");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", systemInstruction);
        body.put("contents", List.of(userTurn));
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return extractTextFromResponse(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            log.warn("Gemini response has no candidates: {}", body.keySet());
            return null;
        }
        Map<String, Object> first = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) first.get("content");
        if (content == null) {
            return null;
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        Object text = parts.get(0).get("text");
        return text != null ? text.toString() : null;
    }
}
