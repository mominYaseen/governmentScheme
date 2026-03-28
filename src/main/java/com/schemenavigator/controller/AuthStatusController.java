package com.schemenavigator.controller;

import com.schemenavigator.auth.AppUserPrincipal;
import com.schemenavigator.auth.DemoUserSessionPrincipal;
import com.schemenavigator.dto.SessionStatusDto;
import com.schemenavigator.model.ApiResponse;
import com.schemenavigator.model.AppUser;
import com.schemenavigator.config.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = OpenApiTags.ACCOUNT, description = "Auth session probe")
public class AuthStatusController {

    /**
     * Avoid calling {@code GET /api/me} before login (that returns 401). Use this for “am I logged in?”.
     * Uses principal type — not only {@code isAuthenticated()} (anonymous is “authenticated” in Spring).
     */
    @GetMapping("/status")
    @Operation(summary = "Session status (always 200)", tags = {OpenApiTags.ACCOUNT})
    public ResponseEntity<ApiResponse<SessionStatusDto>> status(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(new SessionStatusDto(isLoggedIn(authentication))));
    }

    private static boolean isLoggedIn(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        Object p = authentication.getPrincipal();
        if (p == null || "anonymousUser".equals(p)) {
            return false;
        }
        return p instanceof DemoUserSessionPrincipal || p instanceof AppUser || p instanceof AppUserPrincipal;
    }
}
