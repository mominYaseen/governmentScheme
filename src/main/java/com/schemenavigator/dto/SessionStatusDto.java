package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SessionStatus", description = "Whether the browser has an authenticated session (no 401).")
public record SessionStatusDto(
        @Schema(description = "True after successful demo login or OAuth2") boolean authenticated
) {
}
