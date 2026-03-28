package com.schemenavigator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemenavigator.dto.ProfileWithSchemeRanking;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProfileExtractionService {

    private static final String SYSTEM_PROMPT = """
            You are a profile extraction assistant for an Indian government scheme eligibility checker.
            Extract structured information from the user message and return ONLY valid JSON.
            No explanation, no markdown, no code blocks. Just the raw JSON object.

            Return exactly this JSON structure:
            {
              "occupation": "string or null — use one of: farmer, student, vendor, labourer, shopkeeper, government_employee, unemployed, other",
              "income_annual": "number in rupees or null — convert lakhs to rupees, 1 lakh = 100000",
              "location": "string or null — city or district name as mentioned",
              "state": "2-letter state code or null — use JK for any J&K location like Srinagar, Jammu, Baramulla, Sopore, Anantnag, Kupwara, Pulwama, Kargil, Leh, Kathua, Udhampur",
              "gender": "male or female or other or null",
              "land_owned": true or false or null,
              "caste_category": "GEN or OBC or SC or ST or null",
              "age": number or null,
              "bpl_card": true or false or null,
              "is_student": true or false or null — true if occupation is student or mentions college or school,
              "is_farmer": true or false or null — true if occupation is farmer,
              "is_disabled": true or false or null,
              "detected_language": "en or hi or ur or ks"
            }

            Rules:
            - Return null for any field not clearly mentioned. Do not guess or assume.
            - income_annual: if user says "1 lakh" return 100000, "1.5 lakh" return 150000, "80 thousand" return 80000
            - state: if any J&K district or city is mentioned, always set state to JK
            - detected_language: en for English, hi for Hindi/Devanagari, ur for Urdu/Nastaliq, ks for Kashmiri
            - is_farmer must be true whenever occupation is farmer
            - is_student must be true whenever occupation is student or user mentions studying
            """;

    private static final String COMBINED_SYSTEM_PROMPT = """
            You are an assistant for an Indian government scheme eligibility checker.

            From the user message, extract a structured profile AND rank every scheme id you are given by relevance to that message.

            Return ONLY valid JSON (no markdown). Shape:
            {
              "occupation": "string or null — use one of: farmer, student, vendor, labourer, shopkeeper, government_employee, unemployed, other",
              "income_annual": "number in rupees or null — convert lakhs to rupees, 1 lakh = 100000",
              "location": "string or null",
              "state": "2-letter state code or null — use JK for any J&K location like Srinagar, Jammu, Baramulla, Sopore, Anantnag, Kupwara, Pulwama, Kargil, Leh, Kathua, Udhampur",
              "gender": "male or female or other or null",
              "land_owned": true or false or null,
              "caste_category": "GEN or OBC or SC or ST or null",
              "age": number or null,
              "bpl_card": true or false or null,
              "is_student": true or false or null,
              "is_farmer": true or false or null,
              "is_disabled": true or false or null,
              "detected_language": "en or hi or ur or ks",
              "ranked_scheme_ids": ["...", "..."]
            }

            Profile rules:
            - Return null for any field not clearly mentioned. Do not guess.
            - income_annual: "1 lakh" -> 100000, "1.5 lakh" -> 150000
            - state: J&K places -> JK
            - is_farmer true when occupation is farmer; is_student true when student or studying

            Ranking rules:
            - ranked_scheme_ids must list every scheme id from the user message exactly once, most relevant first.
            - Use intent: scholarships, housing, farmers, loans, vendors, women, LPG, SC/ST, entrepreneurship, J&K schemes.
            - Never invent ids.
            """;

    private final GeminiLlmClient geminiLlmClient;
    private final ObjectMapper objectMapper;

    public ProfileExtractionService(GeminiLlmClient geminiLlmClient, ObjectMapper objectMapper) {
        this.geminiLlmClient = geminiLlmClient;
        this.objectMapper = objectMapper;
    }

    public UserProfile extractProfile(String userInput) {
        try {
            String sanitized = userInput.trim();
            if (sanitized.length() > 2000) {
                sanitized = sanitized.substring(0, 2000);
            }

            String jsonText = geminiLlmClient.generate(SYSTEM_PROMPT, sanitized, 500, true);
            if (jsonText == null || jsonText.isBlank()) {
                throw new IllegalStateException("Empty Gemini response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(jsonText.trim(), Map.class);
            return parseProfileFromMap(data, userInput);

        } catch (Exception e) {
            log.error("Gemini profile extraction failed: {}. Falling back to keyword extraction.", e.getMessage());
            return keywordFallbackExtraction(userInput);
        }
    }

    /**
     * One Gemini call: profile fields + scheme relevance order (saves a separate ranking request per match).
     */
    public ProfileWithSchemeRanking extractProfileAndRankSchemes(String userInput, List<Scheme> schemes) {
        List<String> idFallback = schemes.stream().map(Scheme::getId).toList();
        if (schemes.isEmpty()) {
            return new ProfileWithSchemeRanking(keywordFallbackExtraction(userInput), List.of());
        }
        try {
            String sanitized = userInput.trim();
            if (sanitized.length() > 2000) {
                sanitized = sanitized.substring(0, 2000);
            }
            StringBuilder catalog = new StringBuilder();
            for (Scheme s : schemes) {
                catalog.append("- ").append(s.getId()).append(": ").append(s.getName()).append(" — ");
                if (s.getDescription() != null) {
                    catalog.append(s.getDescription());
                }
                catalog.append("\n");
            }
            String userMsg = "User message:\n" + sanitized + "\n\nSchemes (include each id exactly once in ranked_scheme_ids):\n" + catalog;

            String jsonText = geminiLlmClient.generate(COMBINED_SYSTEM_PROMPT, userMsg, 2048, true);
            if (jsonText == null || jsonText.isBlank()) {
                throw new IllegalStateException("Empty Gemini response");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(jsonText.trim(), Map.class);
            UserProfile profile = parseProfileFromMap(data, userInput);
            List<String> ranked = mergeRankingFromResponse(data.get("ranked_scheme_ids"), schemes);
            return new ProfileWithSchemeRanking(profile, ranked);
        } catch (Exception e) {
            log.error("Gemini combined profile+ranking failed: {}. Falling back to keywords + default scheme order.",
                    e.getMessage());
            return new ProfileWithSchemeRanking(keywordFallbackExtraction(userInput), idFallback);
        }
    }

    private List<String> mergeRankingFromResponse(Object rankedRaw, List<Scheme> schemes) {
        List<String> fallback = schemes.stream().map(Scheme::getId).toList();
        if (!(rankedRaw instanceof List<?> list)) {
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
            return fallback;
        }
        return ordered;
    }

    private UserProfile parseProfileFromMap(Map<String, Object> data, String rawInput) {
        try {
            UserProfile.UserProfileBuilder builder = UserProfile.builder();
            builder.rawInput(rawInput);

            if (data.get("occupation") != null) {
                builder.occupation(data.get("occupation").toString().toLowerCase());
            }

            if (data.get("income_annual") != null) {
                try {
                    builder.incomeAnnual(Long.parseLong(data.get("income_annual").toString().replace(".0", "")));
                } catch (NumberFormatException ex) {
                    log.warn("Could not parse income_annual: {}", data.get("income_annual"));
                }
            }

            if (data.get("location") != null) {
                builder.location(data.get("location").toString());
            }

            if (data.get("state") != null) {
                builder.state(data.get("state").toString().toUpperCase());
            }

            if (data.get("gender") != null) {
                builder.gender(data.get("gender").toString().toLowerCase());
            }

            if (data.get("land_owned") != null) {
                builder.landOwned(Boolean.parseBoolean(data.get("land_owned").toString()));
            }

            if (data.get("caste_category") != null) {
                builder.casteCategory(data.get("caste_category").toString().toUpperCase());
            }

            if (data.get("age") != null) {
                try {
                    builder.age(Integer.parseInt(data.get("age").toString().replace(".0", "")));
                } catch (NumberFormatException ex) {
                    log.warn("Could not parse age: {}", data.get("age"));
                }
            }

            if (data.get("bpl_card") != null) {
                builder.bplCard(Boolean.parseBoolean(data.get("bpl_card").toString()));
            }

            if (data.get("is_farmer") != null) {
                builder.isFarmer(Boolean.parseBoolean(data.get("is_farmer").toString()));
            }

            if (data.get("is_student") != null) {
                builder.isStudent(Boolean.parseBoolean(data.get("is_student").toString()));
            }

            if (data.get("is_disabled") != null) {
                builder.isDisabled(Boolean.parseBoolean(data.get("is_disabled").toString()));
            }

            if (data.get("detected_language") != null) {
                builder.detectedLanguage(data.get("detected_language").toString());
            } else {
                builder.detectedLanguage("en");
            }

            UserProfile profile = builder.build();

            if (profile.getIsFarmer() == null && "farmer".equals(profile.getOccupation())) {
                profile.setIsFarmer(true);
            }
            if (profile.getIsStudent() == null && "student".equals(profile.getOccupation())) {
                profile.setIsStudent(true);
            }

            return profile;

        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response: {}.", e.getMessage());
            return keywordFallbackExtraction(rawInput);
        }
    }

    private UserProfile keywordFallbackExtraction(String input) {
        String lower = input.toLowerCase();
        UserProfile.UserProfileBuilder builder = UserProfile.builder();
        builder.rawInput(input);
        builder.detectedLanguage("en");

        if (lower.contains("farmer") || lower.contains("kisan") || lower.contains("krishak") || lower.contains("kheti")) {
            builder.occupation("farmer");
            builder.isFarmer(true);
        } else if (lower.contains("student") || lower.contains("padhai") || lower.contains("college") || lower.contains("school")) {
            builder.occupation("student");
            builder.isStudent(true);
        } else if (lower.contains("vendor") || lower.contains("hawker") || lower.contains("dukaan")) {
            builder.occupation("vendor");
        } else if (lower.contains("labour") || lower.contains("mazdoor")) {
            builder.occupation("labourer");
        }

        if (lower.contains("lakh") || lower.contains("lac")) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)\\s*(lakh|lac)");
            java.util.regex.Matcher m = p.matcher(lower);
            if (m.find()) {
                try {
                    double lakhs = Double.parseDouble(m.group(1));
                    builder.incomeAnnual((long) (lakhs * 100000));
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }

        String[] jkLocations = {"srinagar", "jammu", "baramulla", "sopore", "anantnag",
                "kupwara", "pulwama", "kargil", "leh", "kathua", "udhampur",
                "kashmir", "j&k", "jk"};
        for (String loc : jkLocations) {
            if (lower.contains(loc)) {
                builder.state("JK");
                break;
            }
        }

        if (lower.contains(" sc ") || lower.contains("scheduled caste")) {
            builder.casteCategory("SC");
        } else if (lower.contains(" st ") || lower.contains("scheduled tribe")) {
            builder.casteCategory("ST");
        } else if (lower.contains("obc") || lower.contains("other backward")) {
            builder.casteCategory("OBC");
        }

        if (lower.contains(" female") || lower.contains("woman") || lower.contains("wife") || lower.contains("aurat")) {
            builder.gender("female");
        } else if (lower.contains(" male") || lower.contains("man ") || lower.contains("aadmi")) {
            builder.gender("male");
        }

        if (lower.contains("bpl") || lower.contains("below poverty")) {
            builder.bplCard(true);
        }

        return builder.build();
    }
}
