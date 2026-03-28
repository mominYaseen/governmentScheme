package com.schemenavigator.service;

import com.schemenavigator.model.MatchResult;
import com.schemenavigator.model.MatchedScheme;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.repository.SchemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SchemeMatchingService {

    /** Max schemes returned per category; relevance-only ordering made many prompts look identical — strength-first sort below fixes that. */
    private static final int MAX_ELIGIBLE_RETURN = 12;
    private static final int MAX_NEAR_MISS_RETURN = 6;

    private final SchemeRepository schemeRepository;
    private final EligibilityEngineService eligibilityEngine;
    private final SchemeRelevanceService schemeRelevanceService;

    public SchemeMatchingService(SchemeRepository schemeRepository,
                                 EligibilityEngineService eligibilityEngine,
                                 SchemeRelevanceService schemeRelevanceService) {
        this.schemeRepository = schemeRepository;
        this.eligibilityEngine = eligibilityEngine;
        this.schemeRelevanceService = schemeRelevanceService;
    }

    public List<MatchedScheme> matchAll(UserProfile profile) {
        return matchAll(profile, null);
    }

    /**
     * @param precomputedRelevanceOrder when non-null and non-empty, skips a separate Gemini ranking call (e.g. from combined extract).
     */
    public List<MatchedScheme> matchAll(UserProfile profile, List<String> precomputedRelevanceOrder) {
        List<Scheme> allSchemes = schemeRepository.findAllActiveWithRules();

        if (allSchemes.isEmpty()) {
            log.warn("No active schemes found in database. Check data.sql seeding.");
        }

        String raw = profile.getRawInput() != null ? profile.getRawInput() : "";
        List<String> relevanceOrder = precomputedRelevanceOrder != null && !precomputedRelevanceOrder.isEmpty()
                ? precomputedRelevanceOrder
                : schemeRelevanceService.rankByPrompt(raw, allSchemes);
        Map<String, Integer> relevanceRank = new HashMap<>();
        for (int i = 0; i < relevanceOrder.size(); i++) {
            relevanceRank.put(relevanceOrder.get(i), i);
        }
        int rankFallback = relevanceOrder.size();

        List<MatchedScheme> eligible = new ArrayList<>();
        List<MatchedScheme> nonEligible = new ArrayList<>();

        for (Scheme scheme : allSchemes) {
            MatchResult result = eligibilityEngine.evaluate(profile, scheme);
            MatchedScheme matchedScheme = MatchedScheme.builder()
                    .scheme(scheme)
                    .matchResult(result)
                    .build();

            if (result.isEligible()) {
                eligible.add(matchedScheme);
            } else {
                nonEligible.add(matchedScheme);
            }
        }

        List<MatchedScheme> nearMiss = new ArrayList<>();
        for (MatchedScheme m : nonEligible) {
            if (m.getMatchResult().getEligibilityScore() >= 0.5) {
                nearMiss.add(m);
            }
        }

        if (eligible.isEmpty() && nearMiss.isEmpty() && !nonEligible.isEmpty()) {
            // Prefer schemes closest to passing rules; relevance-only order matched DB/Gemini tie order and repeated every prompt.
            nonEligible.sort((a, b) -> compareByMatchStrengthThenRelevance(a, b, relevanceRank, rankFallback));
            int limit = Math.min(MAX_NEAR_MISS_RETURN, nonEligible.size());
            nearMiss.addAll(nonEligible.subList(0, limit));
        }

        eligible.sort((a, b) -> compareByMatchStrengthThenRelevance(a, b, relevanceRank, rankFallback));

        nearMiss.sort((a, b) -> compareByMatchStrengthThenRelevance(a, b, relevanceRank, rankFallback));

        List<MatchedScheme> result = new ArrayList<>();
        result.addAll(eligible.stream().limit(MAX_ELIGIBLE_RETURN).toList());
        result.addAll(nearMiss.stream().limit(MAX_NEAR_MISS_RETURN).toList());

        log.debug("Matched {} eligible and {} near-miss schemes for profile: {}",
                eligible.size(), nearMiss.size(), profile.getOccupation());

        return result;
    }

    /**
     * Rule-engine strength first so different profiles surface different schemes; LLM rank is a tie-breaker only.
     */
    private static int compareByMatchStrengthThenRelevance(MatchedScheme a, MatchedScheme b,
                                                             Map<String, Integer> relevanceRank, int rankFallback) {
        int cmp = Double.compare(b.getMatchResult().getEligibilityScore(), a.getMatchResult().getEligibilityScore());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(b.getMatchResult().getPassedRules().size(), a.getMatchResult().getPassedRules().size());
        if (cmp != 0) {
            return cmp;
        }
        int ra = relevanceRank.getOrDefault(a.getScheme().getId(), rankFallback);
        int rb = relevanceRank.getOrDefault(b.getScheme().getId(), rankFallback);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        return a.getScheme().getId().compareTo(b.getScheme().getId());
    }
}
