package com.schemenavigator.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyUrlExtractorTest {

    @Test
    void firstFrom_findsUrlInProse() {
        assertThat(ApplyUrlExtractor.firstFrom("Apply at https://example.gov.in/portal/page."))
                .isEqualTo("https://example.gov.in/portal/page");
    }

    @Test
    void resolve_prefersStored() {
        assertThat(ApplyUrlExtractor.resolve(
                "https://stored.example/apply",
                "Also https://other.example/"))
                .isEqualTo("https://stored.example/apply");
    }

    @Test
    void resolve_fallsBackToFirstInText() {
        assertThat(ApplyUrlExtractor.resolve(
                null,
                "No link here",
                "See https://gujarat.gov.in/some-path) for details."))
                .isEqualTo("https://gujarat.gov.in/some-path");
    }

    @Test
    void firstFrom_nullSafe() {
        assertThat(ApplyUrlExtractor.firstFrom((String[]) null)).isNull();
        assertThat(ApplyUrlExtractor.firstFrom("", null)).isNull();
    }
}
