package com.schemenavigator.service;

import com.schemenavigator.model.EligibilityRule;
import com.schemenavigator.model.MatchResult;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class EligibilityEngineService {

    public MatchResult evaluate(UserProfile profile, Scheme scheme) {
        List<String> passedRules = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();
        List<String> skippedRules = new ArrayList<>();
        int mandatoryRuleCount = 0;

        List<EligibilityRule> rules = scheme.getEligibilityRules();
        if (rules == null || rules.isEmpty()) {
            return MatchResult.builder()
                    .eligible(true)
                    .passedRules(passedRules)
                    .failedRules(failedRules)
                    .skippedRules(skippedRules)
                    .eligibilityScore(1.0)
                    .build();
        }

        for (EligibilityRule rule : rules) {
            try {
                if (Boolean.TRUE.equals(rule.getIsMandatory())) {
                    mandatoryRuleCount++;
                }

                Object userValue = getFieldValue(profile, rule.getFieldName());

                if (userValue == null) {
                    skippedRules.add("Unknown: " + rule.getFieldName());
                    continue;
                }

                boolean passed = applyOperator(userValue, rule);

                if (passed) {
                    passedRules.add(buildPassMessage(rule, userValue));
                } else {
                    if (Boolean.TRUE.equals(rule.getIsMandatory())) {
                        failedRules.add(rule.getFailureMessage() != null
                                ? rule.getFailureMessage()
                                : "Failed rule: " + rule.getFieldName());
                    }
                }
            } catch (Exception e) {
                log.warn("Error evaluating rule {} for scheme {}: {}",
                        rule.getFieldName(), scheme.getId(), e.getMessage());
                skippedRules.add("Error evaluating: " + rule.getFieldName());
            }
        }

        boolean eligible = failedRules.isEmpty();
        double score = mandatoryRuleCount > 0
                ? (double) passedRules.size() / mandatoryRuleCount
                : 1.0;
        score = Math.min(score, 1.0);

        return MatchResult.builder()
                .eligible(eligible)
                .passedRules(passedRules)
                .failedRules(failedRules)
                .skippedRules(skippedRules)
                .eligibilityScore(score)
                .build();
    }

    private Object getFieldValue(UserProfile profile, String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "occupation" -> profile.getOccupation();
            case "income_annual" -> profile.getIncomeAnnual();
            case "age" -> profile.getAge();
            case "gender" -> profile.getGender();
            case "land_owned" -> profile.getLandOwned();
            case "caste_category" -> profile.getCasteCategory();
            case "bpl_card" -> profile.getBplCard();
            case "state" -> profile.getState();
            case "is_farmer" -> profile.getIsFarmer();
            case "is_student" -> profile.getIsStudent();
            case "is_disabled" -> profile.getIsDisabled();
            default -> null;
        };
    }

    private boolean applyOperator(Object userValue, EligibilityRule rule) {
        return switch (rule.getOperator().toUpperCase()) {
            case "EQUALS" ->
                    userValue.toString().equalsIgnoreCase(rule.getValueString());

            case "NOT_EQUALS" ->
                    !userValue.toString().equalsIgnoreCase(rule.getValueString());

            case "LESS_THAN" ->
                    toLong(userValue) < rule.getValueNumber().longValue();

            case "LESS_THAN_OR_EQUAL" ->
                    toLong(userValue) <= rule.getValueNumber().longValue();

            case "GREATER_THAN" ->
                    toLong(userValue) > rule.getValueNumber().longValue();

            case "GREATER_THAN_OR_EQUAL" ->
                    toLong(userValue) >= rule.getValueNumber().longValue();

            case "IN" -> {
                if (rule.getValueString() == null) {
                    yield false;
                }
                String[] values = rule.getValueString().split(",");
                yield Arrays.stream(values)
                        .anyMatch(v -> v.trim().equalsIgnoreCase(userValue.toString()));
            }

            case "NOT_IN" -> {
                if (rule.getValueString() == null) {
                    yield true;
                }
                String[] values = rule.getValueString().split(",");
                yield Arrays.stream(values)
                        .noneMatch(v -> v.trim().equalsIgnoreCase(userValue.toString()));
            }

            case "IS_TRUE" ->
                    Boolean.TRUE.equals(userValue) || "true".equalsIgnoreCase(userValue.toString());

            case "IS_FALSE" ->
                    Boolean.FALSE.equals(userValue) || "false".equalsIgnoreCase(userValue.toString());

            default -> {
                log.warn("Unknown operator: {}", rule.getOperator());
                yield false;
            }
        };
    }

    private long toLong(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String buildPassMessage(EligibilityRule rule, Object userValue) {
        return switch (rule.getOperator().toUpperCase()) {
            case "EQUALS" -> rule.getFieldName() + " is " + userValue;
            case "LESS_THAN_OR_EQUAL" -> rule.getFieldName() + " (Rs " + userValue + ") is within limit";
            case "GREATER_THAN_OR_EQUAL" -> rule.getFieldName() + " (" + userValue + ") meets minimum";
            case "IS_TRUE" -> rule.getFieldName() + " confirmed";
            case "IN" -> rule.getFieldName() + " (" + userValue + ") is in eligible category";
            default -> rule.getFieldName() + " check passed";
        };
    }
}
