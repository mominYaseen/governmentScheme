package com.schemenavigator.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
public class ApiRootController {

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> root() {
        return Map.ofEntries(
                Map.entry("service", "scheme-navigator"),
                Map.entry("health", "/api/health"),
                Map.entry("match", Map.of("method", "POST", "path", "/api/schemes/match")),
                Map.entry("schemes", Map.of("method", "GET", "path", "/api/schemes")),
                Map.entry("schemeById", Map.of("method", "GET", "path", "/api/schemes/{id}")),
                Map.entry("recommend", Map.of("method", "POST", "path", "/api/schemes/recommend")),
                Map.entry("authStatus", Map.of("method", "GET", "path", "/api/auth/status")),
                Map.entry("demoLogin", Map.of("method", "POST", "path", "/api/auth/login")),
                Map.entry("demoLogout", Map.of("method", "POST", "path", "/api/auth/logout")),
                Map.entry("oauth2Google", Map.of("method", "GET", "path", "/oauth2/authorization/google")),
                Map.entry("accountMe", Map.of("method", "GET", "path", "/api/me")),
                Map.entry("savedSchemes", Map.of("method", "GET", "path", "/api/me/saved-schemes")),
                Map.entry("openApi", "/v3/api-docs"),
                Map.entry("swaggerUi", "/swagger-ui/index.html"));
    }
}
