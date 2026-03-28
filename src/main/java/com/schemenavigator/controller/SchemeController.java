package com.schemenavigator.controller;

import com.schemenavigator.dto.SchemeMatchResponse;
import com.schemenavigator.dto.UserInputRequest;
import com.schemenavigator.model.ApiResponse;
import com.schemenavigator.model.MatchedScheme;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.SchemeDocument;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.repository.SchemeRepository;
import com.schemenavigator.service.LlmExplanationService;
import com.schemenavigator.service.ProfileExtractionService;
import com.schemenavigator.service.SchemeMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schemes")
@Tag(name = "Schemes", description = "Match government schemes from natural-language profile text")
@Slf4j
public class SchemeController {

    private final ProfileExtractionService profileExtractionService;
    private final SchemeMatchingService schemeMatchingService;
    private final LlmExplanationService llmExplanationService;
    private final SchemeRepository schemeRepository;

    public SchemeController(ProfileExtractionService profileExtractionService,
                            SchemeMatchingService schemeMatchingService,
                            LlmExplanationService llmExplanationService,
                            SchemeRepository schemeRepository) {
        this.profileExtractionService = profileExtractionService;
        this.schemeMatchingService = schemeMatchingService;
        this.llmExplanationService = llmExplanationService;
        this.schemeRepository = schemeRepository;
    }

    @PostMapping("/match")
    @Operation(summary = "Match schemes", description = "Extracts structured profile (Gemini), runs eligibility rules, enriches with LLM explanations.")
    public ResponseEntity<ApiResponse<SchemeMatchResponse>> matchSchemes(
            @Valid @RequestBody UserInputRequest request) {

        long startTime = System.currentTimeMillis();
        log.info("Received match request. Input length: {}", request.getUserInput().length());

        UserProfile profile = profileExtractionService.extractProfile(request.getUserInput());

        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            profile.setDetectedLanguage(request.getLanguage());
        }

        int totalSchemesChecked = schemeRepository.findByIsActiveTrue().size();
        List<MatchedScheme> matchedSchemes = schemeMatchingService.matchAll(profile);

        Map<String, Object> explanationMap = llmExplanationService.generateExplanation(profile, matchedSchemes);

        long processingTimeMs = System.currentTimeMillis() - startTime;

        Map<String, Map<String, Object>> eligibleById = indexExplanationList(explanationMap.get("eligible_explanations"));
        Map<String, Map<String, Object>> nearMissById = indexExplanationList(explanationMap.get("near_miss_explanations"));

        List<SchemeMatchResponse.EligibleSchemeDto> eligibleDtos = new ArrayList<>();
        List<SchemeMatchResponse.NearMissSchemeDto> nearMissDtos = new ArrayList<>();

        for (MatchedScheme ms : matchedSchemes) {
            Scheme s = ms.getScheme();
            if (ms.getMatchResult().isEligible()) {
                Map<String, Object> ex = eligibleById.getOrDefault(s.getId(), Map.of());
                List<String> docs = stringListFromExplanation(ex.get("documents_needed"));
                if (docs.isEmpty()) {
                    docs = documentNames(s);
                }
                String why = stringOrEmpty(ex.get("why_eligible"));
                if (why.isBlank()) {
                    why = "You appear to meet the eligibility criteria for this scheme based on your profile.";
                }
                String how = stringOrEmpty(ex.get("how_to_apply"));
                if (how.isBlank()) {
                    how = s.getApplyProcess() != null ? s.getApplyProcess() : "Visit the official apply link for steps.";
                }
                eligibleDtos.add(SchemeMatchResponse.EligibleSchemeDto.builder()
                        .schemeId(s.getId())
                        .schemeName(firstNonBlank(stringOrEmpty(ex.get("scheme_display_name")), s.getName()))
                        .ministry(firstNonBlank(stringOrEmpty(ex.get("ministry_local")), s.getMinistry()))
                        .benefits(firstNonBlank(stringOrEmpty(ex.get("benefits_local")), s.getBenefits()))
                        .applyUrl(s.getApplyUrl())
                        .whyEligible(why)
                        .howToApply(how)
                        .documentsNeeded(docs)
                        .passedRules(ms.getMatchResult().getPassedRules())
                        .eligibilityScore(ms.getMatchResult().getEligibilityScore())
                        .build());
            } else {
                Map<String, Object> ex = nearMissById.getOrDefault(s.getId(), Map.of());
                String whyNot = stringOrEmpty(ex.get("why_not_eligible"));
                if (whyNot.isBlank()) {
                    whyNot = String.join("; ", ms.getMatchResult().getFailedRules());
                }
                String whatToDo = stringOrEmpty(ex.get("what_to_do"));
                if (whatToDo.isBlank()) {
                    whatToDo = "Review the failed criteria and official guidelines; gather documents if you become eligible.";
                }
                nearMissDtos.add(SchemeMatchResponse.NearMissSchemeDto.builder()
                        .schemeId(s.getId())
                        .schemeName(firstNonBlank(stringOrEmpty(ex.get("scheme_display_name")), s.getName()))
                        .benefits(firstNonBlank(stringOrEmpty(ex.get("benefits_local")), s.getBenefits()))
                        .whyNotEligible(whyNot)
                        .whatToDo(whatToDo)
                        .eligibilityScore(ms.getMatchResult().getEligibilityScore())
                        .build());
            }
        }

        Object summaryObj = explanationMap.get("summary_message");
        String summaryMessage = summaryObj != null ? summaryObj.toString() : "";
        if (summaryMessage.isBlank()) {
            summaryMessage = "Here are schemes that match or nearly match your profile.";
        }

        SchemeMatchResponse body = SchemeMatchResponse.builder()
                .userProfile(profile)
                .eligibleSchemes(eligibleDtos)
                .nearMissSchemes(nearMissDtos)
                .summaryMessage(summaryMessage)
                .detectedLanguage(profile.getDetectedLanguage())
                .processingTimeMs(processingTimeMs)
                .totalSchemesChecked(totalSchemesChecked)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    private Map<String, Map<String, Object>> indexExplanationList(Object raw) {
        Map<String, Map<String, Object>> out = new HashMap<>();
        if (!(raw instanceof List<?> list)) {
            return out;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Object sid = m.get("scheme_id");
                if (sid != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> entry = (Map<String, Object>) m;
                    out.put(sid.toString(), entry);
                }
            }
        }
        return out;
    }

    private List<String> stringListFromExplanation(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).filter(s -> !s.isBlank()).toList();
        }
        return List.of();
    }

    private String stringOrEmpty(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback != null ? fallback : "";
    }

    private List<String> documentNames(Scheme s) {
        if (s.getDocuments() == null) {
            return List.of();
        }
        return s.getDocuments().stream().map(SchemeDocument::getDocumentName).toList();
    }
}
