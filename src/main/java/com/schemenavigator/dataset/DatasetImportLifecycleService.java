package com.schemenavigator.dataset;

import com.schemenavigator.repository.SchemeRepository;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Runs CSV import inside a proper Spring transaction (delete + bulk insert).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DatasetImportLifecycleService {

    private final SchemeRepository schemeRepository;
    private final CsvSchemeDatasetImportService csvSchemeDatasetImportService;

    /**
     * @return negative = skipped; non-negative = number of rows imported
     */
    @Transactional
    public int importOrSkip(Path path, String sourceTag, boolean force) throws IOException, CsvException {
        long existing = schemeRepository.countBySource(sourceTag);
        if (existing > 0 && !force) {
            return -1;
        }
        if (existing > 0) {
            int removed = schemeRepository.deleteBySource(sourceTag);
            log.warn("CSV import (force): removed {} rows with source={}", removed, sourceTag);
        }
        return csvSchemeDatasetImportService.importFromPath(path, sourceTag);
    }
}
