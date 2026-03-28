package com.schemenavigator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemenavigator.model.Scheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orders schemes by semantic relevance to the user's natural-language message (Gemini).
 */
@Service
@Slf4j
public class SchemeRelevanceService {

    private static final String SYSTEM_PROMPT = """
            You rank Indian government schemes by how well they match the citizen's question or situation.

            Return ONLY valid JSON: {"ranked_scheme_ids": ["ID", ...]}.
            The array must list every scheme id from the user's list exactly once, most relevant first.
            Use intent: scholarships, education, housing, farmers, credit, street vendors, women, LPG, SC/ST loans,
            entrepreneurship, skill training, J&K-specific schemes, etc.
            Never invent ids; only use ids you were given.
            """;

    private final GeminiLlmClient geminiLlmClient;
    private final ObjectMapper objectMapper;

    public SchemeRelevanceService(GeminiLlmClient geminiLlmClient, ObjectMapper objectMapper) {
        this.geminiLlmClient = geminiLlmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @return scheme ids in relevance order (same ids as {@code schemes}); falls back to repository order on failure.
     */
    @SuppressWarnings("unchecked")
    public List<String> rankByPrompt(String userMessage, List<Scheme> schemes) {
        List<String> fallback = schemes.stream().map(Scheme::getId).toList();
        if (userMessage == null || userMessage.isBlank() || schemes.isEmpty()) {
            return fallback;
        }
        String trimmed = userMessage.length() > 3000 ? userMessage.substring(0, 3000) : userMessage;
        try {
            StringBuilder catalog = new StringBuilder();
            for (Scheme s : schemes) {
                catalog.append("- ").append(s.getId()).append(": ").append(s.getName()).append(" — ");
                if (s.getDescription() != null) {
                    catalog.append(s.getDescription());
                }
                catalog.append("\n");
            }
            String user = "User message:\n" + trimmed + "\n\nSchemes to rank (include each id exactly once):\n" + catalog;

            String jsonText = geminiLlmClient.generate(SYSTEM_PROMPT, user, 1024, true);
            if (jsonText == null || jsonText.isBlank()) {
                return fallback;
            }

            Map<String, Object> root = objectMapper.readValue(jsonText.trim(), Map.class);
            Object raw = root.get("ranked_scheme_ids");
            if (!(raw instanceof List<?> list)) {
                return fallback;
            }

            List<String> ordered = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String id = o.toString().trim();
                if (!id.isEmpty() && !seen.contains(id)) {
                    seen.add(id);
                    ordered.add(id);
                }
            }

            Set<String> expected = schemes.stream().map(Scheme::getId).collect(Collectors.toSet());
            ordered.removeIf(id -> !expected.contains(id));
            seen.clear();
            seen.addAll(ordered);

            for (String id : fallback) {
                if (!seen.contains(id)) {
                    ordered.add(id);
                    seen.add(id);
                }
            }

            if (ordered.size() != expected.size()) {
                log.warn("Relevance ranking size mismatch; using fallback order");
                return fallback;
            }
            return ordered;
        } catch (Exception e) {
            log.warn("Scheme relevance ranking failed: {}", e.getMessage());
            return fallback;
        }
    }
}
