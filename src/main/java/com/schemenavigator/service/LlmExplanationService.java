package com.schemenavigator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemenavigator.model.MatchedScheme;
import com.schemenavigator.model.SchemeDocument;
import com.schemenavigator.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmExplanationService {

    private static final String SYSTEM_PROMPT = """
            You are a friendly and empathetic government scheme advisor helping Indian citizens,
            especially from rural and semi-urban areas of Jammu and Kashmir.

            The user chose a RESPONSE LANGUAGE (field "Language to respond in" in the user message).
            You must write ALL human-readable strings in that language so the UI can show a fully localized experience.

            Language codes:
            - en: English
            - hi: Hindi (Devanagari)
            - ur: Urdu (Nastaliq / Arabic script)
            - ks: Kashmiri (use Hindi in Devanagari if Kashmiri is difficult)

            Translation rules:
            - scheme_display_name: natural title in the response language (you may add the short English acronym in parentheses if helpful, e.g. for PM-KISAN).
            - ministry_local: translate the ministry / department name.
            - benefits_local: translate the full benefits description; keep amounts and rupee figures accurate.
            - why_eligible, how_to_apply, why_not_eligible, what_to_do: fully in the response language.
            - documents_needed: translate each document name for the user; keep meaning identical to the English list you were given.
            - summary_message: in the response language.
            - Apply URLs must stay unchanged when echoed inside how_to_apply or what_to_do (full https URL as given).

            Keep explanations concise (under ~180 words per scheme block).

            You must respond with ONLY a valid JSON object. No markdown, no code fences.
            """;

    private final GeminiLlmClient geminiLlmClient;
    private final ObjectMapper objectMapper;

    public LlmExplanationService(GeminiLlmClient geminiLlmClient, ObjectMapper objectMapper) {
        this.geminiLlmClient = geminiLlmClient;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateExplanation(UserProfile profile, List<MatchedScheme> matchedSchemes) {
        try {
            String userMessage = buildUserMessage(profile, matchedSchemes);

            String jsonText = geminiLlmClient.generate(SYSTEM_PROMPT, userMessage, 4096, true);
            if (jsonText == null || jsonText.isBlank()) {
                throw new IllegalStateException("Empty Gemini response");
            }

            return objectMapper.readValue(jsonText.trim(), Map.class);

        } catch (Exception e) {
            log.error("Gemini explanation failed: {}. Returning fallback.", e.getMessage());
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

                Respond with this exact JSON structure (all string values in the response language above):
                {
                  "eligible_explanations": [
                    {
                      "scheme_id": "...",
                      "scheme_display_name": "...",
                      "ministry_local": "...",
                      "benefits_local": "...",
                      "why_eligible": "...",
                      "how_to_apply": "...",
                      "documents_needed": ["...", "..."]
                    }
                  ],
                  "near_miss_explanations": [
                    {
                      "scheme_id": "...",
                      "scheme_display_name": "...",
                      "benefits_local": "...",
                      "why_not_eligible": "...",
                      "what_to_do": "..."
                    }
                  ],
                  "summary_message": "1-2 encouraging sentences in the response language"
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
                        "scheme_display_name", "",
                        "ministry_local", "",
                        "benefits_local", "",
                        "why_eligible", "You meet the eligibility criteria for this scheme.",
                        "how_to_apply", ms.getScheme().getApplyProcess() != null
                                ? ms.getScheme().getApplyProcess() : "Visit the official website to apply.",
                        "documents_needed", docs
                ));
            } else {
                nearMissExplanations.add(Map.of(
                        "scheme_id", ms.getScheme().getId(),
                        "scheme_display_name", "",
                        "benefits_local", "",
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
