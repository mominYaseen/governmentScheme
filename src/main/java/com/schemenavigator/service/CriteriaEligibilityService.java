package com.schemenavigator.service;

import com.schemenavigator.config.DatasetImportProperties;
import com.schemenavigator.model.EligibilityCriteria;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based matching using {@link EligibilityCriteria} rows (structured columns, not legacy {@code eligibility_rules}).
 * A scheme is eligible if it has at least one criteria row and at least one row matches (OR). Within a row, all
 * non-null constraints must pass (AND).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CriteriaEligibilityService {

    private final SchemeRepository schemeRepository;
    private final DatasetImportProperties datasetImportProperties;

    public List<Scheme> findEligibleSchemes(UserProfile profile) {
        String tag = datasetImportProperties.getSourceTag() != null
                ? datasetImportProperties.getSourceTag()
                : "KAGGLE_CSV";
        List<Scheme> candidates = schemeRepository.findActiveBySourceWithCriteria(tag);
        List<Scheme> out = new ArrayList<>();
        for (Scheme s : candidates) {
            if (matchesAnyCriteria(profile, s)) {
                out.add(s);
            }
        }
        return out;
    }

    public boolean matchesAnyCriteria(UserProfile profile, Scheme scheme) {
        Set<EligibilityCriteria> rows = scheme.getEligibilityCriteria();
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (EligibilityCriteria c : rows) {
            if (rowMatches(profile, c)) {
                return true;
            }
        }
        return false;
    }

    public boolean rowMatches(UserProfile profile, EligibilityCriteria c) {
        return incomeMatches(profile, c)
                && stateMatches(profile, c)
                && occupationMatches(profile, c)
                && genderMatches(profile, c);
    }

    private boolean incomeMatches(UserProfile profile, EligibilityCriteria c) {
        Long income = profile.getIncomeAnnual();
        if (c.getMinIncomeAnnual() != null) {
            if (income == null) {
                return false;
            }
            if (income < c.getMinIncomeAnnual()) {
                return false;
            }
        }
        if (c.getMaxIncomeAnnual() != null) {
            if (income == null) {
                return false;
            }
            if (income > c.getMaxIncomeAnnual()) {
                return false;
            }
        }
        return true;
    }

    private boolean stateMatches(UserProfile profile, EligibilityCriteria c) {
        String codes = c.getStateCodes();
        if (codes == null || codes.isBlank()) {
            return true;
        }
        String userState = profile.getState();
        if (userState == null || userState.isBlank()) {
            return false;
        }
        Set<String> allowed = Arrays.stream(codes.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return allowed.contains(userState.trim().toUpperCase(Locale.ROOT));
    }

    private boolean occupationMatches(UserProfile profile, EligibilityCriteria c) {
        String occField = c.getOccupations();
        if (occField == null || occField.isBlank()) {
            return true;
        }
        String userOcc = profile.getOccupation();
        if (userOcc == null || userOcc.isBlank()) {
            if (Boolean.TRUE.equals(profile.getIsFarmer())) {
                userOcc = "farmer";
            } else if (Boolean.TRUE.equals(profile.getIsStudent())) {
                userOcc = "student";
            }
        }
        if (userOcc == null || userOcc.isBlank()) {
            return false;
        }
        String u = userOcc.toLowerCase(Locale.ROOT).trim();
        return Arrays.stream(occField.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .anyMatch(token -> u.equals(token) || u.contains(token) || token.contains(u));
    }

    private boolean genderMatches(UserProfile profile, EligibilityCriteria c) {
        String g = c.getGender();
        if (g == null || g.isBlank()) {
            return true;
        }
        String user = profile.getGender();
        if (user == null || user.isBlank()) {
            return false;
        }
        return user.trim().equalsIgnoreCase(g.trim());
    }
}
