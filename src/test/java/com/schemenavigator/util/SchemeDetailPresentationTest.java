package com.schemenavigator.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemeDetailPresentationTest {

    @Test
    void overview_usesDescriptionWhenClean() {
        assertThat(SchemeDetailPresentation.computeOverview(
                "Skill training for construction workers in AP.",
                "Step-1: Visit office.\nStep-2: Submit form.",
                "Stipend ₹7000."))
                .isEqualTo("Skill training for construction workers in AP.");
    }

    @Test
    void overview_stripsEmbeddedApplyProcess() {
        String apply = "Step-1: Visit board. Step-2: Submit.";
        String desc = "Intro line. " + apply;
        assertThat(SchemeDetailPresentation.computeOverview(desc, apply, "Benefits here."))
                .isEqualTo("Intro line.");
    }

    @Test
    void overview_whenDescriptionIsMostlySteps_usesBenefits() {
        String benefits = "Stipend of ₹7,000. Training and tool kits.";
        assertThat(SchemeDetailPresentation.computeOverview(
                "Step-1: Visit office. Step-2: Fill form.",
                "Step-1: Visit office. Step-2: Fill form.",
                benefits))
                .isEqualTo(benefits);
    }

    @Test
    void overview_emptyDescription_fallsBackToBenefits() {
        assertThat(SchemeDetailPresentation.computeOverview(null, null, "Only benefits text."))
                .isEqualTo("Only benefits text.");
    }

    @Test
    void parseApplySteps_splitsStepLabels() {
        assertThat(SchemeDetailPresentation.parseApplySteps(
                "Step-1: Visit the board. Step-2: Submit the form. Step-3: Collect receipt."))
                .containsExactly(
                        "Step-1: Visit the board.",
                        "Step-2: Submit the form.",
                        "Step-3: Collect receipt.");
    }

    @Test
    void parseApplySteps_splitsNumberedLines() {
        String text = """
                1. First action
                2. Second action
                3. Third action""";
        assertThat(SchemeDetailPresentation.parseApplySteps(text))
                .containsExactly(
                        "1. First action",
                        "2. Second action",
                        "3. Third action");
    }

    @Test
    void parseApplySteps_plainParagraph_singleItem() {
        assertThat(SchemeDetailPresentation.parseApplySteps("Apply online at the portal."))
                .isEqualTo(List.of("Apply online at the portal."));
    }

    @Test
    void parseApplySteps_blank_emptyList() {
        assertThat(SchemeDetailPresentation.parseApplySteps(null)).isEmpty();
        assertThat(SchemeDetailPresentation.parseApplySteps("   ")).isEmpty();
    }
}
