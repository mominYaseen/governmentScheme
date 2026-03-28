package com.schemenavigator.controller;

import com.schemenavigator.dto.ProfileWithSchemeRanking;
import com.schemenavigator.dto.SchemeMatchResponse;
import com.schemenavigator.dto.SchemeDetailDto;
import com.schemenavigator.dto.SchemeSummaryDto;
import com.schemenavigator.dto.UserInputRequest;
import com.schemenavigator.dto.UserProfileRequest;
import com.schemenavigator.model.ApiResponse;
import com.schemenavigator.model.MatchedScheme;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.SchemeDocument;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.repository.SchemeRepository;
import com.schemenavigator.service.CriteriaEligibilityService;
import com.schemenavigator.service.SchemeCatalogService;
import com.schemenavigator.service.LlmExplanationService;
import com.schemenavigator.config.OpenApiTags;
import com.schemenavigator.service.ProfileExtractionService;
import com.schemenavigator.service.SchemeMatchingService;
import com.schemenavigator.util.ApplyUrlExtractor;
import com.schemenavigator.util.SchemeDetailPresentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Schemes (overview)", description = "Use the **catalog** or **AI** sub-tags in each operation for the right flow.")
@Slf4j
public class SchemeController {

    private final ProfileExtractionService profileExtractionService;
    private final SchemeMatchingService schemeMatchingService;
    private final LlmExplanationService llmExplanationService;
    private final SchemeRepository schemeRepository;
    private final CriteriaEligibilityService criteriaEligibilityService;
    private final SchemeCatalogService schemeCatalogService;

    public SchemeController(ProfileExtractionService profileExtractionService,
                            SchemeMatchingService schemeMatchingService,
                            LlmExplanationService llmExplanationService,
                            SchemeRepository schemeRepository,
                            CriteriaEligibilityService criteriaEligibilityService,
                            SchemeCatalogService schemeCatalogService) {
        this.profileExtractionService = profileExtractionService;
        this.schemeMatchingService = schemeMatchingService;
        this.llmExplanationService = llmExplanationService;
        this.schemeRepository = schemeRepository;
        this.criteriaEligibilityService = criteriaEligibilityService;
        this.schemeCatalogService = schemeCatalogService;
    }

    @GetMapping
    @Operation(
            operationId = "listActiveSchemes",
            summary = "List active schemes (paginated)",
            description = """
                    Returns **all active** schemes from every `source` (seed + CSV import).

                    `ApiResponse.data` is a Spring Data `Page<SchemeSummary>`:
                    - `content` — array of schemes (`levelBadge`, `cardSubtitle`, `categories`, … — see schema)
                    - `totalElements`, `totalPages`, `number`, `size`, `first`, `last`, `sort`, …

                    **Query parameters** (standard `Pageable`):
                    - `page` — 0-based page index (default `0`)
                    - `size` — page size (default `20`)
                    - `sort` — e.g. `name,asc` or `id,desc` (repeatable in some clients)
                    """,
            tags = {OpenApiTags.SCHEMES_CATALOG})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list wrapped in ApiResponse"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SchemeSummaryDto>>> listSchemes(
            @ParameterObject
            @PageableDefault(size = 20, sort = "name")
            Pageable pageable) {
        Page<SchemeSummaryDto> page = schemeCatalogService.pageSummaries(pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    @Operation(
            operationId = "getSchemeById",
            summary = "Get one scheme (full text for summary / detail)",
            description = """
                    Returns **overview** (brief narrative), **applySteps** (parsed), plus raw **description** / **applyProcess**,
                    **benefits**, **eligibility** text, **documents**, etc.
                    Use **overview** for an Overview section and **applySteps** (or **applyProcess**) for How to apply.

                    List endpoints (`GET /api/schemes`, `POST /api/schemes/recommend`) return only `SchemeSummary` fields.
                    """,
            tags = {OpenApiTags.SCHEMES_CATALOG})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Scheme detail wrapped in ApiResponse",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SchemeDetailDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Unknown or inactive scheme id", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    })
    public ResponseEntity<ApiResponse<SchemeDetailDto>> getSchemeById(
            @PathVariable
            @io.swagger.v3.oas.annotations.Parameter(description = "Scheme id (same as in list/recommend)", example = "psgs-deg")
            String id) {
        return schemeRepository.findByIdWithDocuments(id)
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .map(s -> ResponseEntity.ok(ApiResponse.ok(toDetail(s))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/recommend")
    @Operation(
            operationId = "recommendSchemesByProfile",
            summary = "Recommend schemes (structured SQL rules)",
            description = """
                    **Rule-based** recommendation using rows in table `eligibility_criteria` (populated from CSV import).

                    A scheme is returned if it has at least one criteria row and **at least one row matches** the profile
                    (OR across rows). Within a row, all non-null constraints must pass (AND).

                    Schemes without any structured criteria row are **never** returned here (use `/match` for broader AI flow).

                    Response: `ApiResponse.data` is `SchemeSummary[]`.
                    """,
            tags = {OpenApiTags.SCHEMES_CATALOG})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Eligible schemes (empty list if none match)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<SchemeSummaryDto>>> recommend(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Structured user profile; omit or null unknown fields",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "farmer_jk",
                                            summary = "Farmer in J&K",
                                            value = """
                                                    {
                                                      "occupation": "farmer",
                                                      "incomeAnnual": 180000,
                                                      "state": "JK",
                                                      "gender": "male",
                                                      "isFarmer": true
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "student_sc",
                                            summary = "Student, low income",
                                            value = """
                                                    {
                                                      "occupation": "student",
                                                      "incomeAnnual": 200000,
                                                      "state": "DL",
                                                      "casteCategory": "SC",
                                                      "isStudent": true
                                                    }
                                                    """)
                            }))
            @RequestBody
            UserProfileRequest request) {
        UserProfile profile = request.toUserProfile();
        List<SchemeSummaryDto> list = schemeCatalogService.summarizeSchemes(
                criteriaEligibilityService.findEligibleSchemes(profile));
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/match")
    @Operation(
            operationId = "matchSchemesNaturalLanguage",
            summary = "Match schemes (natural language + Gemini)",
            description = """
                    **AI-assisted** flow:
                    1. **Gemini** extracts a structured profile and ranks schemes by relevance (single combined call).
                    2. Legacy **eligibility_rules** on **seed** schemes determine eligible vs near-miss.
                    3. **Gemini** again generates localized explanations for the matched set.

                    Requires `GEMINI_API_KEY`. On Gemini failure, the service falls back to keyword extraction and static text.

                    Response: `ApiResponse.data` is `SchemeMatchResponse` (profile, eligible list, near-miss list, summary).
                    """,
            tags = {OpenApiTags.SCHEMES_AI})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Match result with explanations",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SchemeMatchResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error (e.g. empty `userInput`)",
                    content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    })
    public ResponseEntity<ApiResponse<SchemeMatchResponse>> matchSchemes(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User story in plain language",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserInputRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "kashmiri_farmer",
                                            value = """
                                                    {
                                                      "userInput": "I am a woman farmer in Sopore, Jammu and Kashmir. My family income is about 1 lakh per year. I belong to OBC.",
                                                      "language": "en"
                                                    }
                                                    """)
                            }))
            @RequestBody
            UserInputRequest request) {

        long startTime = System.currentTimeMillis();
        log.info("Received match request. Input length: {}", request.getUserInput().length());

        List<Scheme> schemesForRanking = schemeRepository.findAllActiveWithRules();
        ProfileWithSchemeRanking extracted = profileExtractionService.extractProfileAndRankSchemes(
                request.getUserInput(), schemesForRanking);
        UserProfile profile = extracted.profile();

        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            profile.setDetectedLanguage(request.getLanguage());
        }

        int totalSchemesChecked = schemeRepository.findByIsActiveTrue().size();
        List<MatchedScheme> matchedSchemes = schemeMatchingService.matchAll(profile, extracted.rankedSchemeIds());

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
                String overview = SchemeDetailPresentation.computeOverview(
                        s.getDescription(), s.getApplyProcess(), s.getBenefits());
                List<String> applySteps = SchemeDetailPresentation.parseApplySteps(how);
                eligibleDtos.add(SchemeMatchResponse.EligibleSchemeDto.builder()
                        .schemeId(s.getId())
                        .schemeName(firstNonBlank(stringOrEmpty(ex.get("scheme_display_name")), s.getName()))
                        .ministry(firstNonBlank(stringOrEmpty(ex.get("ministry_local")), s.getMinistry()))
                        .benefits(firstNonBlank(stringOrEmpty(ex.get("benefits_local")), s.getBenefits()))
                        .applyUrl(ApplyUrlExtractor.resolve(
                                s.getApplyUrl(), s.getApplyProcess(), s.getDescription(), s.getBenefits()))
                        .overview(overview)
                        .whyEligible(why)
                        .howToApply(how)
                        .applySteps(applySteps)
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

    private SchemeDetailDto toDetail(Scheme s) {
        String desc = s.getDescription();
        String apply = s.getApplyProcess();
        String ben = s.getBenefits();
        String applyUrl = ApplyUrlExtractor.resolve(s.getApplyUrl(), apply, desc, ben);
        return new SchemeDetailDto(
                s.getId(),
                s.getName(),
                s.getSlug(),
                s.getGovLevel(),
                s.getSource(),
                s.getMinistry(),
                desc,
                SchemeDetailPresentation.computeOverview(desc, apply, ben),
                ben,
                apply,
                SchemeDetailPresentation.parseApplySteps(apply),
                applyUrl,
                s.getEligibilityRaw(),
                s.getTags(),
                documentNames(s));
    }
}
