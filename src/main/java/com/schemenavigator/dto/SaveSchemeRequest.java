package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "SaveSchemeRequest")
public record SaveSchemeRequest(
        @NotBlank
        @Schema(description = "Scheme id from catalog / recommend / match", example = "pm-kisan")
        String schemeId,
        @Schema(description = "If false, no reminder emails for this saved row", nullable = true)
        Boolean remindEnabled
) {
}
