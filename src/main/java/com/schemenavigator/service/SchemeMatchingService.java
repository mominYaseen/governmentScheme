package com.schemenavigator.service;

import com.schemenavigator.model.MatchResult;
import com.schemenavigator.model.MatchedScheme;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.repository.SchemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SchemeMatchingService {

    private final SchemeRepository schemeRepository;
    private final EligibilityEngineService eligibilityEngine;

    public SchemeMatchingService(SchemeRepository schemeRepository,
                                 EligibilityEngineService eligibilityEngine) {
        this.schemeRepository = schemeRepository;
        this.eligibilityEngine = eligibilityEngine;
    }

    public List<MatchedScheme> matchAll(UserProfile profile) {
        List<Scheme> allSchemes = schemeRepository.findAllActiveWithRules();

        if (allSchemes.isEmpty()) {
            log.warn("No active schemes found in database. Check data.sql seeding.");
        }

        List<MatchedScheme> eligible = new ArrayList<>();
        List<MatchedScheme> nearMiss = new ArrayList<>();

        for (Scheme scheme : allSchemes) {
            MatchResult result = eligibilityEngine.evaluate(profile, scheme);
            MatchedScheme matchedScheme = MatchedScheme.builder()
                    .scheme(scheme)
                    .matchResult(result)
                    .build();

            if (result.isEligible()) {
                eligible.add(matchedScheme);
            } else if (result.getEligibilityScore() >= 0.5) {
                nearMiss.add(matchedScheme);
            }
        }

        eligible.sort((a, b) ->
                b.getMatchResult().getPassedRules().size() - a.getMatchResult().getPassedRules().size());

        nearMiss.sort((a, b) ->
                Double.compare(b.getMatchResult().getEligibilityScore(),
                        a.getMatchResult().getEligibilityScore()));

        List<MatchedScheme> result = new ArrayList<>();
        result.addAll(eligible.stream().limit(5).toList());
        result.addAll(nearMiss.stream().limit(3).toList());

        log.debug("Matched {} eligible and {} near-miss schemes for profile: {}",
                eligible.size(), nearMiss.size(), profile.getOccupation());

        return result;
    }
}
