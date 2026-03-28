package com.schemenavigator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {
    private boolean eligible;
    private List<String> passedRules;
    private List<String> failedRules;
    private List<String> skippedRules;
    private double eligibilityScore;
}
