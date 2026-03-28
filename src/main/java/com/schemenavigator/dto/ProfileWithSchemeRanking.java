package com.schemenavigator.dto;

import com.schemenavigator.model.UserProfile;

import java.util.List;

/**
 * Result of a single Gemini call: structured profile plus scheme ids ordered by relevance to the user message.
 */
public record ProfileWithSchemeRanking(UserProfile profile, List<String> rankedSchemeIds) {
}
