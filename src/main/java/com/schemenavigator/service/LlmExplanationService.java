package com.schemenavigator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemenavigator.config.AppConfig;
import com.schemenavigator.model.MatchedScheme;
import com.schemenavigator.model.SchemeDocument;
import com.schemenavigator.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmExplanationService {

    private static final String SYSTEM_PROMPT = """
            You are a friendly and empathetic government scheme advisor helping Indian citizens,
            especially from rural and semi-urban areas of Jammu and Kashmir.

            Explain eligibility results in simple, warm, encouraging language.
            Avoid jargon. Be specific and actionable.
            Keep explanations under 150 words per scheme.

            Always respond in the language specified. Language codes:
            - en: English
            - hi: Hindi in Devanagari script
            - ur: Urdu in Nastaliq script
            - ks: Kashmiri (use Hindi if Kashmiri is not possible)

            Always keep scheme names, URLs, and document names in English even when responding in other languages.

            You must respond with ONLY a valid JSON object. No explanation, no markdown, no code blocks.
            """;

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    public LlmExplanationService(RestTemplate restTemplate, AppConfig appConfig, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateExplanation(UserProfile profile, List<MatchedScheme> matchedSchemes) {
        try {
            String userMessage = buildUserMessage(profile, matchedSchemes);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", appConfig.getOpenAiModel());
            requestBody.put("max_tokens", 1500);
            requestBody.put("messages", messages);
            requestBody.put("response_format", Map.of("type", "json_object"));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + appConfig.getOpenAiApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    appConfig.getOpenAiApiUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            String jsonText = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            return objectMapper.readValue(jsonText, Map.class);

        } catch (Exception e) {
            log.error("LLM explanation failed: {}. Returning fallback.", e.getMessage());
            return buildFallbackExplanation(matchedSchemes);
        }
    }

    private String buildUserMessage(UserProfile profile, List<MatchedScheme> matchedSchemes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Language to respond in: ").append(profile.getDetectedLanguage()).append("\n\n");
        sb.append("User profile:\n");
        if (profile.getOccupation() != null) {
            sb.append("- Occupation: ").append(profile.getOccupation()).append("\n");
        }
        if (profile.getIncomeAnnual() != null) {
            sb.append("- Annual income: Rs ").append(profile.getIncomeAnnual()).append("\n");
        }
        if (profile.getLocation() != null) {
            sb.append("- Location: ").append(profile.getLocation()).append("\n");
        }
        if (profile.getState() != null) {
            sb.append("- State: ").append(profile.getState()).append("\n");
        }
        if (profile.getCasteCategory() != null) {
            sb.append("- Caste: ").append(profile.getCasteCategory()).append("\n");
        }
        if (profile.getAge() != null) {
            sb.append("- Age: ").append(profile.getAge()).append("\n");
        }
        if (profile.getGender() != null) {
            sb.append("- Gender: ").append(profile.getGender()).append("\n");
        }
        if (profile.getBplCard() != null) {
            sb.append("- BPL card: ").append(profile.getBplCard()).append("\n");
        }

        List<MatchedScheme> eligible = matchedSchemes.stream()
                .filter(m -> m.getMatchResult().isEligible()).toList();
        List<MatchedScheme> nearMiss = matchedSchemes.stream()
                .filter(m -> !m.getMatchResult().isEligible()).toList();

        sb.append("\nEligible schemes:\n");
        for (MatchedScheme ms : eligible) {
            sb.append("- ").append(ms.getScheme().getName())
                    .append(" (ID: ").append(ms.getScheme().getId()).append(")\n");
            sb.append("  Benefits: ").append(ms.getScheme().getBenefits()).append("\n");
            sb.append("  Apply: ").append(ms.getScheme().getApplyProcess()).append("\n");
            sb.append("  Passed rules: ").append(ms.getMatchResult().getPassedRules()).append("\n");
            sb.append("  Documents: ");
            if (ms.getScheme().getDocuments() != null) {
                ms.getScheme().getDocuments().forEach(d -> sb.append(d.getDocumentName()).append(", "));
            }
            sb.append("\n");
        }

        sb.append("\nNear-miss schemes (almost eligible):\n");
        for (MatchedScheme ms : nearMiss) {
            sb.append("- ").append(ms.getScheme().getName())
                    .append(" (ID: ").append(ms.getScheme().getId()).append(")\n");
            sb.append("  Failed rules: ").append(ms.getMatchResult().getFailedRules()).append("\n");
        }

        sb.append("""

                Respond with this exact JSON structure:
                {
                  "eligible_explanations": [
                    {
                      "scheme_id": "...",
                      "why_eligible": "...",
                      "how_to_apply": "...",
                      "documents_needed": ["...", "..."]
                    }
                  ],
                  "near_miss_explanations": [
                    {
                      "scheme_id": "...",
                      "why_not_eligible": "...",
                      "what_to_do": "..."
                    }
                  ],
                  "summary_message": "1-2 sentence encouraging summary in the detected language"
                }
                """);

        return sb.toString();
    }

    private Map<String, Object> buildFallbackExplanation(List<MatchedScheme> matchedSchemes) {
        List<Map<String, Object>> eligibleExplanations = new ArrayList<>();
        List<Map<String, Object>> nearMissExplanations = new ArrayList<>();

        for (MatchedScheme ms : matchedSchemes) {
            if (ms.getMatchResult().isEligible()) {
                List<String> docs = ms.getScheme().getDocuments() == null
                        ? List.of()
                        : ms.getScheme().getDocuments().stream()
                        .map(SchemeDocument::getDocumentName).toList();

                eligibleExplanations.add(Map.of(
                        "scheme_id", ms.getScheme().getId(),
                        "why_eligible", "You meet the eligibility criteria for this scheme.",
                        "how_to_apply", ms.getScheme().getApplyProcess() != null
                                ? ms.getScheme().getApplyProcess() : "Visit the official website to apply.",
                        "documents_needed", docs
                ));
            } else {
                nearMissExplanations.add(Map.of(
                        "scheme_id", ms.getScheme().getId(),
                        "why_not_eligible", String.join("; ", ms.getMatchResult().getFailedRules()),
                        "what_to_do", "Review the eligibility criteria and gather required documents."
                ));
            }
        }

        return Map.of(
                "eligible_explanations", eligibleExplanations,
                "near_miss_explanations", nearMissExplanations,
                "summary_message", "Here are the schemes we found for you. Please visit the official websites to apply."
        );
    }
}
