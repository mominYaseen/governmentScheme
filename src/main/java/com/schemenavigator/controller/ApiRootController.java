package com.schemenavigator.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiRootController {

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> root() {
        return Map.of(
                "service", "scheme-navigator",
                "health", "/api/health",
                "match", Map.of("method", "POST", "path", "/api/schemes/match"),
                "openApi", "/v3/api-docs",
                "swaggerUi", "/swagger-ui/index.html");
    }
}
