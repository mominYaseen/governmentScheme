package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "DemoLoginRequest", description = "Demo auth when app.auth.type=demo")
public record DemoLoginRequest(
        @NotBlank @Email @Schema(example = "demo@example.com") String email,
        @NotBlank @Schema(example = "demo123") String password
) {
}
