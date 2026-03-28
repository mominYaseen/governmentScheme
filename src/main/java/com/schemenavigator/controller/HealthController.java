package com.schemenavigator.controller;

import com.schemenavigator.config.OpenApiTags;
import com.schemenavigator.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = OpenApiTags.HEALTH, description = "Liveness and readiness-style checks for orchestration and frontend connectivity tests.")
public class HealthController {

    @GetMapping
    @Operation(
            operationId = "healthCheck",
            summary = "Health check",
            description = """
                    Returns `ApiResponse.data` as a small map with `status=UP` and service name.
                    Use before wiring the main UI to scheme endpoints.
                    """,
            tags = {OpenApiTags.HEALTH})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service is running",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "up",
                                    value = """
                                            {
                                              "success": true,
                                              "data": { "status": "UP", "service": "scheme-navigator" },
                                              "error": null,
                                              "timestamp": "2026-03-28T12:00:00Z"
                                            }
                                            """)))
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP", "service", "scheme-navigator")));
    }
}
