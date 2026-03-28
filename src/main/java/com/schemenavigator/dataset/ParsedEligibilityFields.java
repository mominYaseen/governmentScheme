package com.schemenavigator.dataset;

import lombok.Builder;
import lombok.Value;

/**
 * Structured fields heuristically extracted from free-text eligibility (CSV column).
 */
@Value
@Builder
public class ParsedEligibilityFields {
    Long minIncomeAnnual;
    Long maxIncomeAnnual;
    String stateCodes;
    String occupations;
    String gender;

    boolean hasAnyConstraint() {
        return minIncomeAnnual != null
                || maxIncomeAnnual != null
                || (stateCodes != null && !stateCodes.isBlank())
                || (occupations != null && !occupations.isBlank())
                || (gender != null && !gender.isBlank());
    }
}
