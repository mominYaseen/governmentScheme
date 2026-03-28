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
            nonEligible.sort((a, b) -> compareByRelevanceThenScore(a, b, relevanceRank, rankFallback));
            int limit = Math.min(5, nonEligible.size());
            nearMiss.addAll(nonEligible.subList(0, limit));
        }

        eligible.sort((a, b) -> compareByRelevanceThenPassedRules(a, b, relevanceRank, rankFallback));

        nearMiss.sort((a, b) -> compareByRelevanceThenScore(a, b, relevanceRank, rankFallback));

        List<MatchedScheme> result = new ArrayList<>();
        result.addAll(eligible.stream().limit(5).toList());
        result.addAll(nearMiss.stream().limit(3).toList());

        log.debug("Matched {} eligible and {} near-miss schemes for profile: {}",
                eligible.size(), nearMiss.size(), profile.getOccupation());

        return result;
    }

    private static int compareByRelevanceThenPassedRules(MatchedScheme a, MatchedScheme b,
                                                         Map<String, Integer> relevanceRank, int rankFallback) {
        int ra = relevanceRank.getOrDefault(a.getScheme().getId(), rankFallback);
        int rb = relevanceRank.getOrDefault(b.getScheme().getId(), rankFallback);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        return Integer.compare(
                b.getMatchResult().getPassedRules().size(),
                a.getMatchResult().getPassedRules().size());
    }

    private static int compareByRelevanceThenScore(MatchedScheme a, MatchedScheme b,
                                                   Map<String, Integer> relevanceRank, int rankFallback) {
        int ra = relevanceRank.getOrDefault(a.getScheme().getId(), rankFallback);
        int rb = relevanceRank.getOrDefault(b.getScheme().getId(), rankFallback);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        return Double.compare(
                b.getMatchResult().getEligibilityScore(),
                a.getMatchResult().getEligibilityScore());
    }
}
