package com.schemenavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.dataset.import")
public class DatasetImportProperties {

    /**
     * When true, import runs once at startup if CSV path is set and DB has no rows for {@link #sourceTag}.
     */
    private boolean enabled = false;

    /** Absolute or {@code file:} path to {@code updated_data.csv} */
    private String path = "";

    /** Value stored on {@code schemes.source} for imported rows */
    private String sourceTag = "KAGGLE_CSV";

    /**
     * When true, deletes existing rows with {@link #sourceTag} before import (use once to refresh CSV data).
     */
    private boolean force = false;
}
