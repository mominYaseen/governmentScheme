package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "SavedSchemeItem", description = "A scheme saved to the user inventory.")
public record SavedSchemeItemDto(
        @Schema(description = "Row id in user_saved_schemes") long savedId,
        @Schema(description = "When the user saved this scheme") Instant savedAt,
        @Schema(description = "Whether email reminders are enabled for this row") boolean remindEnabled,
        @Schema(description = "When the next reminder is scheduled (UTC), if any", nullable = true)
        Instant nextReminderAt,
        @Schema(description = "Scheme summary (same shape as catalog list)")
        SchemeSummaryDto scheme
) {
}
