package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "SchemeSummary",
        description = """
                Lightweight scheme row for catalog lists and `/recommend` results.

                **UI hints:** use `levelBadge` for programme level (Central/State), not `source`. \
                Use `cardSubtitle` or `categories` under the title, not the raw `slug`.""")
public record SchemeSummaryDto(
        @Schema(description = "Primary key / stable id (slug-based for CSV imports)", example = "pm-kisan")
        String id,
        @Schema(description = "Official or dataset display name")
        String name,
        @Schema(
                description = """
                        URL / internal key derived from slug. **Do not** show as the main subtitle when \
                        `cardSubtitle` or `categories` are present.""")
        String slug,
        @Schema(description = "Raw level from dataset (`Central` | `State`)", example = "State", nullable = true)
        String govLevel,
        @Schema(
                description = """
                        Import / pipeline tag (e.g. KAGGLE_CSV, LEGACY). **Not** an Indian state name — \
                        never pair with a \"State:\" badge label.""")
        String source,
        @Schema(description = "Apply URL when known (often null for CSV rows)", nullable = true)
        String applyUrl,
        @Schema(description = "Up to 5 category labels from the dataset (for chips or subtitle).")
        List<String> categories,
        @Schema(
                nullable = true,
                description = "Human-readable line under the title: first category or a level phrase.")
        String cardSubtitle,
        @Schema(
                nullable = true,
                description = "Short badge: \"Central\" or \"State\". Prefer this over `source` for any level badge.")
        String levelBadge
) {
}
