package com.schemenavigator.dataset;

import com.schemenavigator.config.DatasetImportProperties;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Order(100)
@Slf4j
@RequiredArgsConstructor
public class DatasetImportRunner implements ApplicationRunner {

    /** Used when {@code app.dataset.import.path} is blank (e.g. empty env var overrides). */
    private static final String DEFAULT_CSV_PATH = "/home/momin/Downloads/archive/updated_data.csv";

    private final DatasetImportProperties properties;
    private final DatasetImportLifecycleService datasetImportLifecycleService;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("=== CSV dataset import runner === enabled={} force={} configuredPath='{}'",
                properties.isEnabled(), properties.isForce(), properties.getPath());

        if (!properties.isEnabled()) {
            log.warn("=== CSV dataset import: disabled (app.dataset.import.enabled=false) ===");
            return;
        }

        String rawPath = properties.getPath();
        String p = rawPath != null ? rawPath.trim() : "";
        if (p.isEmpty()) {
            p = DEFAULT_CSV_PATH;
            log.warn("=== CSV dataset import: path was empty; using default {} ===", p);
        }

        String tag = properties.getSourceTag() != null ? properties.getSourceTag() : "KAGGLE_CSV";
        Path path = Paths.get(p.replaceFirst("^file:", ""));
        if (!Files.isRegularFile(path)) {
            log.error("=== CSV dataset import: file not found at {} (absolute: {}) ===",
                    p, path.toAbsolutePath());
            return;
        }

        try {
            int n = datasetImportLifecycleService.importOrSkip(path, tag, properties.isForce());
            if (n < 0) {
                log.warn("=== CSV dataset import SKIPPED: KAGGLE rows already present (source={}). "
                        + "Set app.dataset.import.force=true once to replace. ===", tag);
                return;
            }
            log.warn("=== CSV IMPORT DONE: {} schemes imported (source={}) ===", n, tag);
        } catch (IOException | CsvException e) {
            log.error("=== CSV dataset import FAILED: {} ===", e.getMessage(), e);
        }
    }
}
