package com.schemenavigator.dto;

import com.schemenavigator.model.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "SchemeMatchResponse",
        description = "Payload inside `ApiResponse.data` for `POST /api/schemes/match` (AI + legacy rules).")
public class SchemeMatchResponse {

    @Schema(description = "Structured profile extracted or inferred from `userInput`")
    private UserProfile userProfile;

    @Schema(description = "Schemes that passed mandatory eligibility rules")
    private List<EligibleSchemeDto> eligibleSchemes;

    @Schema(description = "Schemes that almost qualify (e.g. high partial score) but failed at least one mandatory rule")
    private List<NearMissSchemeDto> nearMissSchemes;

    @Schema(description = "Short encouraging summary (possibly translated per `language`)")
    private String summaryMessage;

    @Schema(description = "Language code used for localized explanation fields", example = "en")
    private String detectedLanguage;

    @Schema(description = "Server processing time for the match call in milliseconds", example = "2500")
    private long processingTimeMs;

    @Schema(description = "Count of active schemes considered in the engine", example = "10")
    private int totalSchemesChecked;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "EligibleScheme")
    public static class EligibleSchemeDto {

        @Schema(example = "PM-KISAN")
        private String schemeId;

        @Schema(description = "Display name (may be localized by Gemini)")
        private String schemeName;

        @Schema(description = "Ministry / department label")
        private String ministry;

        @Schema(description = "Benefits summary")
        private String benefits;

        @Schema(description = "Official apply URL when available", nullable = true)
        private String applyUrl;

        @Schema(
                description = """
                        Neutral scheme overview (from catalog description/benefits). Use for an **Overview** card — \
                        do not concatenate `whyEligible` or `howToApply` here.""")
        private String overview;

        @Schema(description = "Why the user is considered eligible")
        private String whyEligible;

        @Schema(description = "Full how-to-apply text (LLM or catalog apply_process)")
        private String howToApply;

        @Schema(description = "Parsed application steps; prefer rendering these under **How to apply**")
        private List<String> applySteps;

        @Schema(description = "Required documents as a list of names")
        private List<String> documentsNeeded;

        @Schema(description = "Human-readable list of rules that evaluated to pass")
        private List<String> passedRules;

        @Schema(description = "0.0–1.0 score from the rule engine for this scheme", example = "1.0")
        private double eligibilityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "NearMissScheme")
    public static class NearMissSchemeDto {

        @Schema(example = "JKEDI-LOAN")
        private String schemeId;

        @Schema(description = "Display name (may be localized)")
        private String schemeName;

        @Schema(description = "Benefits summary")
        private String benefits;

        @Schema(description = "Why the user is not fully eligible")
        private String whyNotEligible;

        @Schema(description = "Actionable next steps")
        private String whatToDo;

        @Schema(description = "0.0–1.0 partial eligibility score", example = "0.5")
        private double eligibilityScore;
    }
}
