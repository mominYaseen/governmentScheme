package com.schemenavigator.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds UI-oriented fields so "Overview" stays high-level and "How to apply" stays step-by-step,
 * even when the dataset mixes copy or the frontend previously concatenated fields.
 */
public final class SchemeDetailPresentation {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern STARTS_WITH_STEP = Pattern.compile("(?is)^\\s*Step[-\\s]*\\d+\\s*[:.)]");
    private static final Pattern STEP_SEGMENT_BOUNDARY = Pattern.compile("(?i)(?=Step[-\\s]*\\d+\\s*[:.)])");
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^\\s*(\\d+)[.)]\\s+");

    private SchemeDetailPresentation() {
    }

    /**
     * Short scheme narrative for an Overview card — not step-by-step application instructions.
     */
    public static String computeOverview(String description, String applyProcess, String benefits) {
        String d = normalize(description);
        String a = normalize(applyProcess);
        String b = normalize(benefits);

        if (d.isEmpty()) {
            return pickFallbackOverview(b);
        }

        if (!a.isEmpty() && d.contains(a)) {
            d = normalize(d.replace(a, " "));
        }

        if (STARTS_WITH_STEP.matcher(d).find() && !b.isEmpty()) {
            return pickFallbackOverview(b);
        }

        if (d.isEmpty()) {
            return pickFallbackOverview(b);
        }
        return d;
    }

    /**
     * Ordered application steps. When the text is not step-structured, returns a single-element list
     * so the UI can still render one block under "How to apply".
     */
    public static List<String> parseApplySteps(String applyProcess) {
        if (applyProcess == null || applyProcess.isBlank()) {
            return List.of();
        }
        String text = applyProcess.trim().replace("\r\n", "\n");

        String[] stepParts = STEP_SEGMENT_BOUNDARY.split(text);
        List<String> stepChunks = Arrays.stream(stepParts)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (stepChunks.size() > 1
                || (stepChunks.size() == 1 && STARTS_WITH_STEP.matcher(stepChunks.get(0)).find())) {
            return stepChunks;
        }

        Matcher m = NUMBERED_LINE.matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
        }
        if (starts.size() >= 2) {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < starts.size(); i++) {
                int from = starts.get(i);
                int to = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
                out.add(text.substring(from, to).trim());
            }
            return out;
        }

        return List.of(text.trim());
    }

    private static String pickFallbackOverview(String normalizedBenefits) {
        if (normalizedBenefits == null || normalizedBenefits.isEmpty()) {
            return null;
        }
        return truncateToReadableLength(normalizedBenefits, 720);
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return WHITESPACE.matcher(s.replace("\r\n", "\n").trim()).replaceAll(" ").trim();
    }

    private static String truncateToReadableLength(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        int cut = text.lastIndexOf('.', maxChars);
        if (cut >= 120) {
            return text.substring(0, cut + 1).trim();
        }
        return text.substring(0, maxChars).trim() + "…";
    }
}
