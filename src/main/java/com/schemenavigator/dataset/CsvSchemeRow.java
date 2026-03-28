package com.schemenavigator.dataset;

/**
 * One logical row from {@code updated_data.csv}.
 */
public record CsvSchemeRow(
        String schemeName,
        String slug,
        String details,
        String benefits,
        String eligibility,
        String application,
        String documents,
        String level,
        String schemeCategory,
        String tags
) {
}
