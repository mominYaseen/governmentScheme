package com.schemenavigator.controller;

import com.schemenavigator.auth.AccountAuthSupport;
import com.schemenavigator.dto.AccountProfileDto;
import com.schemenavigator.model.ApiResponse;
import com.schemenavigator.model.AppUser;
import com.schemenavigator.repository.AppUserRepository;
import com.schemenavigator.config.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/me")
@Tag(name = OpenApiTags.ACCOUNT, description = "Signed-in user (demo or OAuth2 session + CSRF cookie).")
public class AccountController {

    private final AppUserRepository appUserRepository;

    public AccountController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    @Operation(
            summary = "Current account",
            description = "Requires a login session (demo or OAuth2). Returns 401 if anonymous.",
            tags = {OpenApiTags.ACCOUNT})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not signed in", content = @Content)
    })
    public ResponseEntity<ApiResponse<AccountProfileDto>> me(Authentication authentication) {
        AppUser u = AccountAuthSupport.requireAppUser(authentication, appUserRepository);
        AccountProfileDto dto = new AccountProfileDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getProvider());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}
