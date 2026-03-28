package com.schemenavigator.dto;

import com.schemenavigator.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemeMatchResponse {
    private UserProfile userProfile;
    private List<EligibleSchemeDto> eligibleSchemes;
    private List<NearMissSchemeDto> nearMissSchemes;
    private String summaryMessage;
    private String detectedLanguage;
    private long processingTimeMs;
    private int totalSchemesChecked;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibleSchemeDto {
        private String schemeId;
        private String schemeName;
        private String ministry;
        private String benefits;
        private String applyUrl;
        private String whyEligible;
        private String howToApply;
        private List<String> documentsNeeded;
        private List<String> passedRules;
        private double eligibilityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearMissSchemeDto {
        private String schemeId;
        private String schemeName;
        private String benefits;
        private String whyNotEligible;
        private String whatToDo;
        private double eligibilityScore;
    }
}
