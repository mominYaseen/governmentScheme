package com.schemenavigator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemenavigator.config.AppConfig;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.service.ProfileExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileExtractionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AppConfig appConfig;

    private ProfileExtractionService service;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        when(appConfig.getOpenAiModel()).thenReturn("gpt-4o");
        when(appConfig.getOpenAiApiUrl()).thenReturn("https://api.openai.com/v1/chat/completions");
        when(appConfig.getOpenAiApiKey()).thenReturn("test-key");
        service = new ProfileExtractionService(restTemplate, appConfig, mapper);
    }

    @Test
    void fallsBackToKeywordsWhenOpenAiFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        UserProfile p = service.extractProfile("I am a farmer in Srinagar earning 1 lakh per year");

        assertEquals("farmer", p.getOccupation());
        assertEquals("JK", p.getState());
        assertEquals(100_000L, p.getIncomeAnnual());
        assertTrue(Boolean.TRUE.equals(p.getIsFarmer()));
    }
}
