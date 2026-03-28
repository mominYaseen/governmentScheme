package com.schemenavigator.service;

import com.schemenavigator.dto.SchemeSummaryDto;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.repository.SchemeRepository;
import com.schemenavigator.util.ApplyUrlExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchemeCatalogService {

    private static final int MAX_CATEGORIES = 5;

    private final SchemeRepository schemeRepository;

    @Transactional(readOnly = true)
    public Page<SchemeSummaryDto> pageSummaries(Pageable pageable) {
        Page<Scheme> page = schemeRepository.findByIsActiveTrue(pageable);
        Map<String, List<String>> byScheme = loadCategoriesBySchemeId(
                page.getContent().stream().map(Scheme::getId).toList());
        return page.map(s -> toSummary(s, byScheme.getOrDefault(s.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public List<SchemeSummaryDto> summarizeSchemes(List<Scheme> schemes) {
        if (schemes.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> byScheme = loadCategoriesBySchemeId(
                schemes.stream().map(Scheme::getId).toList());
        return schemes.stream()
                .map(s -> toSummary(s, byScheme.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    private Map<String, List<String>> loadCategoriesBySchemeId(List<String> schemeIds) {
        if (schemeIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = schemeRepository.findCategoryNamesBySchemeIds(schemeIds);
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String schemeId = (String) row[0];
            String categoryName = (String) row[1];
            if (schemeId == null || categoryName == null || categoryName.isBlank()) {
                continue;
            }
            map.computeIfAbsent(schemeId, k -> new ArrayList<>()).add(categoryName.trim());
        }
        return map;
    }

    private static SchemeSummaryDto toSummary(Scheme s, List<String> categoryNames) {
        List<String> categories = categoryNames.stream().distinct().limit(MAX_CATEGORIES).toList();
        String primary = categories.isEmpty() ? null : categories.get(0);
        String levelBadge = formatLevelBadge(s.getGovLevel());
        String cardSubtitle = buildCardSubtitle(primary, s.getGovLevel());
        String applyUrl = ApplyUrlExtractor.resolve(
                s.getApplyUrl(), s.getApplyProcess(), s.getDescription(), s.getBenefits());
        return new SchemeSummaryDto(
                s.getId(),
                s.getName(),
                s.getSlug(),
                s.getGovLevel(),
                s.getSource(),
                applyUrl,
                categories,
                cardSubtitle,
                levelBadge);
    }

    private static String buildCardSubtitle(String primaryCategory, String govLevel) {
        if (primaryCategory != null && !primaryCategory.isBlank()) {
            return primaryCategory;
        }
        return formatLevelSubtitle(govLevel);
    }

    /** One line under the title when there is no category label. */
    private static String formatLevelSubtitle(String govLevel) {
        if (govLevel == null || govLevel.isBlank()) {
            return null;
        }
        String g = govLevel.trim();
        if (g.equalsIgnoreCase("central")) {
            return "Central government scheme";
        }
        if (g.equalsIgnoreCase("state")) {
            return "State government scheme";
        }
        return g + " scheme";
    }

    /** Short label for a corner badge (not geographic state names). */
    private static String formatLevelBadge(String govLevel) {
        if (govLevel == null || govLevel.isBlank()) {
            return null;
        }
        String g = govLevel.trim();
        if (g.equalsIgnoreCase("central")) {
            return "Central";
        }
        if (g.equalsIgnoreCase("state")) {
            return "State";
        }
        if (g.length() == 1) {
            return g.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(g.charAt(0)) + g.substring(1).toLowerCase(Locale.ROOT);
    }
}
