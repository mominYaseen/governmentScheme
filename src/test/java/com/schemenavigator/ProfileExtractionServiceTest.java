package com.schemenavigator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.service.GeminiLlmClient;
import com.schemenavigator.service.ProfileExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileExtractionServiceTest {

    @Mock
    private GeminiLlmClient geminiLlmClient;

    private ProfileExtractionService service;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        service = new ProfileExtractionService(geminiLlmClient, mapper);
    }

    @Test
    void fallsBackToKeywordsWhenGeminiFails() {
        when(geminiLlmClient.generate(anyString(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("down"));

        UserProfile p = service.extractProfile("I am a farmer in Srinagar earning 1 lakh per year");

        assertEquals("farmer", p.getOccupation());
        assertEquals("JK", p.getState());
        assertEquals(100_000L, p.getIncomeAnnual());
        assertTrue(Boolean.TRUE.equals(p.getIsFarmer()));
    }
}
