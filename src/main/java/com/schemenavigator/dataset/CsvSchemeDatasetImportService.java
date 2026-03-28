package com.schemenavigator.dataset;

import com.schemenavigator.model.*;
import com.schemenavigator.repository.CategoryRepository;
import com.schemenavigator.util.ApplyUrlExtractor;
import com.schemenavigator.repository.SchemeRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvSchemeDatasetImportService {

    private final SchemeRepository schemeRepository;
    private final CategoryRepository categoryRepository;
    private final EligibilityTextParser eligibilityTextParser;

    @Transactional
    public int importFromPath(Path path, String sourceTag) throws IOException, CsvException {
        List<String[]> rows;
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVReader reader = new CSVReader(r)) {
            rows = reader.readAll();
        }
        if (rows.isEmpty()) {
            return 0;
        }
        Map<String, Integer> slugUsage = new HashMap<>();
        int imported = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 10) {
                row = Arrays.copyOf(row, 11);
            }
            CsvSchemeRow dto = new CsvSchemeRow(
                    safe(row, 0),
                    safe(row, 1),
                    safe(row, 2),
                    safe(row, 3),
                    safe(row, 4),
                    safe(row, 5),
                    safe(row, 6),
                    safe(row, 7),
                    safe(row, 8),
                    row.length > 10 ? safe(row, 10) : ""
            );
            if (dto.schemeName().isBlank() || dto.slug().isBlank()) {
                continue;
            }
            persistRow(dto, sourceTag, slugUsage);
            imported++;
        }
        log.info("CSV import finished: {} schemes from {}", imported, path);
        return imported;
    }

    private static String safe(String[] row, int idx) {
        if (idx >= row.length || row[idx] == null) {
            return "";
        }
        return row[idx].trim();
    }

    private void persistRow(CsvSchemeRow r, String sourceTag, Map<String, Integer> slugUsage) {
        String base = sanitizeSlug(r.slug());
        if (base.isEmpty()) {
            base = "scheme-" + UUID.randomUUID().toString().substring(0, 8);
        }
        int n = slugUsage.merge(base, 1, (a, b) -> a + b);
        String id = n == 1 ? base : base + "-" + n;

        Scheme scheme = new Scheme();
        scheme.setId(id);
        scheme.setSlug(id);
        scheme.setName(truncate(r.schemeName(), 510));
        scheme.setDescription(blankToNull(r.details()));
        scheme.setMinistry(null);
        scheme.setBenefits(blankToNull(r.benefits()));
        scheme.setApplyProcess(blankToNull(r.application()));
        scheme.setApplyUrl(blankToNull(ApplyUrlExtractor.firstFrom(r.application(), r.details(), r.benefits())));
        scheme.setIsActive(true);
        scheme.setGovLevel(blankToNull(r.level()));
        scheme.setEligibilityRaw(blankToNull(r.eligibility()));
        scheme.setTags(blankToNull(r.tags()));
        scheme.setSource(sourceTag);

        ParsedEligibilityFields parsed = eligibilityTextParser.parse(r.eligibility());
        if (parsed.hasAnyConstraint()) {
            EligibilityCriteria c = EligibilityCriteria.builder()
                    .scheme(scheme)
                    .minIncomeAnnual(parsed.getMinIncomeAnnual())
                    .maxIncomeAnnual(parsed.getMaxIncomeAnnual())
                    .stateCodes(parsed.getStateCodes())
                    .occupations(parsed.getOccupations())
                    .gender(parsed.getGender())
                    .notes("heuristic_parse")
                    .build();
            scheme.getEligibilityCriteria().add(c);
        }

        for (String docLine : splitDocuments(r.documents())) {
            SchemeDocument d = new SchemeDocument();
            d.setScheme(scheme);
            d.setDocumentName(truncate(docLine, 253));
            d.setIsMandatory(true);
            scheme.getDocuments().add(d);
        }

        attachCategories(scheme, r.schemeCategory());

        schemeRepository.save(scheme);
    }

    private void attachCategories(Scheme scheme, String rawCategories) {
        if (rawCategories == null || rawCategories.isBlank()) {
            return;
        }
        for (String part : rawCategories.split(",")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            Category cat = categoryRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
            SchemeCategory link = SchemeCategory.builder()
                    .scheme(scheme)
                    .category(cat)
                    .build();
            scheme.getSchemeCategories().add(link);
        }
    }

    private static List<String> splitDocuments(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\r?\\n|(?<=\\n)(?=\\d+\\.)|(?=•)"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceFirst("^•\\s*", "").trim())
                .filter(s -> !s.isBlank())
                .limit(40)
                .collect(Collectors.toList());
    }

    private static String sanitizeSlug(String slug) {
        String s = slug.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (s.length() > 120) {
            s = s.substring(0, 120).replaceAll("-+$", "");
        }
        return s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
