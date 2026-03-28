package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "SchemeDetail",
        description = "Full scheme text for detail views and summary modals (catalog or after recommend).")
public record SchemeDetailDto(
        @Schema(description = "Scheme id") String id,
        @Schema(description = "Display name") String name,
        @Schema(nullable = true) String slug,
        @Schema(description = "Central | State", nullable = true) String govLevel,
        @Schema(nullable = true) String source,
        @Schema(nullable = true) String ministry,
        @Schema(description = "Raw long description from storage (CSV: details column)", nullable = true)
        String description,
        @Schema(
                nullable = true,
                description = """
                        Brief scheme overview for an **Overview** section — excludes duplicated application steps \
                        when possible. Prefer this over `description` in the UI.""")
        String overview,
        @Schema(description = "Benefits narrative", nullable = true) String benefits,
        @Schema(description = "Raw how-to-apply text from storage (CSV: application column)", nullable = true)
        String applyProcess,
        @Schema(
                description = """
                        Application steps in order, parsed from `applyProcess` (or LLM `howToApply` on match). \
                        Use for the **How to apply** section; fall back to a single block from `applyProcess` when empty.""")
        List<String> applySteps,
        @Schema(
                nullable = true,
                description = "Official apply link when known; if the DB column is empty, the API may infer the first http(s) URL from apply/description/benefits text.")
        String applyUrl,
        @Schema(description = "Raw eligibility text from dataset", nullable = true) String eligibilityText,
        @Schema(nullable = true) String tags,
        @Schema(description = "Document names when present") List<String> documentsNeeded
) {
}
