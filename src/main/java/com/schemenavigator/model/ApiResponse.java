package com.schemenavigator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ApiResponse",
        description = """
                Standard JSON envelope for almost all endpoints.
                On success: `success=true`, `data` contains the payload, `error` is null.
                On failure: `success=false`, `error` contains a message, `data` is typically null.
                """)
public class ApiResponse<T> {

    @Schema(description = "True when the request completed successfully", example = "true")
    private boolean success;

    @Schema(description = "Response body; concrete shape depends on the operation (see operation-specific schemas)")
    private T data;

    @Schema(description = "Human-readable error when success is false", nullable = true)
    private String error;

    @Schema(description = "ISO-8601 timestamp when the response was built", example = "2026-03-28T12:00:00Z")
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(Instant.now().toString())
                .build();
    }
}
