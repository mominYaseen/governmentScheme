package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccountProfile", description = "Signed-in user (OAuth).")
public record AccountProfileDto(
        @Schema(description = "Internal user id") long id,
        @Schema(description = "Email from the identity provider") String email,
        @Schema(description = "Display name when provided by the provider", nullable = true) String displayName,
        @Schema(description = "OAuth provider id, e.g. google") String provider
) {
}
