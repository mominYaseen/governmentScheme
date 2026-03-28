package com.schemenavigator.dataset;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort extraction of filterable fields from narrative eligibility text.
 * Many schemes will yield {@link ParsedEligibilityFields#hasAnyConstraint()} false — those get no DB row.
 */
@Component
public class EligibilityTextParser {

    private static final Pattern RUPEE_AMOUNT = Pattern.compile(
            "[₹Rs]\\.?\\s*([\\d,]+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAKH_PHRASE = Pattern.compile(
            "(below|less than|not exceeding|not exceed|upto|up to|maximum|under)\\s+(?:Rs\\.?|₹)?\\s*([\\d,.]+)\\s*(lakh|lac)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ABOVE_LAKH = Pattern.compile(
            "(above|more than|exceeding|minimum)\\s+(?:Rs\\.?|₹)?\\s*([\\d,.]+)\\s*(lakh|lac)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> PHRASE_TO_STATE = Map.ofEntries(
            Map.entry("jammu and kashmir", "JK"),
            Map.entry("ladakh", "LA"),
            Map.entry("himachal pradesh", "HP"),
            Map.entry("punjab", "PB"),
            Map.entry("haryana", "HR"),
            Map.entry("delhi", "DL"),
            Map.entry("uttar pradesh", "UP"),
            Map.entry("madhya pradesh", "MP"),
            Map.entry("maharashtra", "MH"),
            Map.entry("gujarat", "GJ"),
            Map.entry("rajasthan", "RJ"),
            Map.entry("bihar", "BR"),
            Map.entry("west bengal", "WB"),
            Map.entry("odisha", "OD"),
            Map.entry("orissa", "OD"),
            Map.entry("karnataka", "KA"),
            Map.entry("tamil nadu", "TN"),
            Map.entry("kerala", "KL"),
            Map.entry("telangana", "TS"),
            Map.entry("andhra pradesh", "AP"),
            Map.entry("assam", "AS"),
            Map.entry("jharkhand", "JH"),
            Map.entry("chhattisgarh", "CG"),
            Map.entry("uttarakhand", "UK"),
            Map.entry("goa", "GA"),
            Map.entry("tripura", "TR"),
            Map.entry("meghalaya", "ML"),
            Map.entry("manipur", "MN"),
            Map.entry("nagaland", "NL"),
            Map.entry("mizoram", "MZ"),
            Map.entry("arunachal pradesh", "AR"),
            Map.entry("sikkim", "SK"),
            Map.entry("puducherry", "PY"),
            Map.entry("pondicherry", "PY")
    );

    public ParsedEligibilityFields parse(String text) {
        if (text == null || text.isBlank()) {
            return ParsedEligibilityFields.builder().build();
        }
        String t = text.toLowerCase(Locale.ROOT);
        Long maxIncome = extractMaxIncomeLakh(t);
        Long minIncome = extractMinIncomeLakh(t);
        if (maxIncome == null) {
            maxIncome = extractMaxIncomePlainDigits(text);
        }
        String states = extractStates(t);
        String occ = extractOccupations(t);
        String gender = extractGender(t);

        return ParsedEligibilityFields.builder()
                .minIncomeAnnual(minIncome)
                .maxIncomeAnnual(maxIncome)
                .stateCodes(states)
                .occupations(occ)
                .gender(gender)
                .build();
    }

    private Long extractMaxIncomeLakh(String lower) {
        Matcher m = LAKH_PHRASE.matcher(lower);
        Long best = null;
        while (m.find()) {
            long v = lakhToRupees(m.group(2));
            if (v > 0) {
                best = best == null ? v : Math.max(best, v);
            }
        }
        return best;
    }

    private Long extractMinIncomeLakh(String lower) {
        Matcher m = ABOVE_LAKH.matcher(lower);
        Long best = null;
        while (m.find()) {
            long v = lakhToRupees(m.group(2));
            if (v > 0) {
                best = best == null ? v : Math.min(best, v);
            }
        }
        return best;
    }

    private long lakhToRupees(String num) {
        try {
            double d = Double.parseDouble(num.replace(",", "").trim());
            return Math.round(d * 100_000L);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Long extractMaxIncomePlainDigits(String original) {
        if (!original.toLowerCase(Locale.ROOT).matches("(?s).*(below|less than|not exceeding|income|annual family).*")) {
            return null;
        }
        Matcher m = RUPEE_AMOUNT.matcher(original);
        long max = 0L;
        while (m.find()) {
            String g = m.group(1).replace(",", "");
            try {
                double d = Double.parseDouble(g);
                if (d > max) {
                    max = Math.round(d);
                }
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return max > 0 && max < 1_000_000_000L ? max : null;
    }

    private String extractStates(String lower) {
        Set<String> codes = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : PHRASE_TO_STATE.entrySet()) {
            if (lower.contains(e.getKey())) {
                codes.add(e.getValue());
            }
        }
        if (codes.isEmpty()) {
            return null;
        }
        return String.join(",", codes);
    }

    private String extractOccupations(String lower) {
        Set<String> occ = new LinkedHashSet<>();
        if (lower.contains("farmer") || lower.contains("cultivat") || lower.contains("kisan")) {
            occ.add("farmer");
        }
        if (lower.contains("fisher")) {
            occ.add("fisherman");
        }
        if (lower.contains("student") || lower.contains("scholar")) {
            occ.add("student");
        }
        if (lower.contains("construction worker") || lower.contains("labour") || lower.contains("labor")) {
            occ.add("labourer");
        }
        if (lower.contains("street vendor") || lower.contains("hawker")) {
            occ.add("vendor");
        }
        if (lower.contains("women") && lower.contains("entrepreneur")) {
            occ.add("other");
        }
        if (occ.isEmpty()) {
            return null;
        }
        return String.join(",", occ);
    }

    private String extractGender(String lower) {
        if (lower.matches("(?s).*(only for women|female beneficiaries|woman beneficiary|for women\\b|girl students).*")) {
            return "female";
        }
        if (lower.matches("(?s).*(only for men|male beneficiaries|for men\\b).*")) {
            return "male";
        }
        return null;
    }
}
