package com.schemenavigator.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dataset rows often omit a dedicated apply URL; links appear inside long text fields.
 */
public final class ApplyUrlExtractor {

    private static final Pattern HTTP_URL = Pattern.compile(
            "(?i)(https?://[^\\s<>\"'\\]\\)]+)",
            Pattern.CASE_INSENSITIVE);

    private ApplyUrlExtractor() {
    }

    /**
     * Returns stored URL if non-blank; otherwise the first http(s) URL found in the given texts (in order).
     */
    public static String resolve(String storedApplyUrl, String... texts) {
        if (storedApplyUrl != null && !storedApplyUrl.isBlank()) {
            return storedApplyUrl.trim();
        }
        return firstFrom(texts);
    }

    public static String firstFrom(String... texts) {
        if (texts == null) {
            return null;
        }
        for (String t : texts) {
            if (t == null || t.isBlank()) {
                continue;
            }
            Matcher m = HTTP_URL.matcher(t);
            if (m.find()) {
                return trimTrailingPunctuation(m.group(1));
            }
        }
        return null;
    }

    private static String trimTrailingPunctuation(String url) {
        String u = url.trim();
        while (!u.isEmpty()) {
            char c = u.charAt(u.length() - 1);
            if (c == '.' || c == ',' || c == ';' || c == ':' || c == ')' || c == ']' || c == '}'
                    || c == '"' || c == '\'' || c == '»' || c == '’') {
                u = u.substring(0, u.length() - 1).trim();
            } else {
                break;
            }
        }
        return u.isEmpty() ? null : u;
    }
}
