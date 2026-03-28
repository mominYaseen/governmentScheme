package com.schemenavigator.controller;

import com.schemenavigator.auth.AccountAuthSupport;
import com.schemenavigator.dto.SaveSchemeRequest;
import com.schemenavigator.dto.SavedSchemeItemDto;
import com.schemenavigator.model.ApiResponse;
import com.schemenavigator.model.AppUser;
import com.schemenavigator.repository.AppUserRepository;
import com.schemenavigator.service.UserSavedSchemeService;
import com.schemenavigator.config.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/me/saved-schemes")
@Tag(name = OpenApiTags.ACCOUNT, description = "Saved scheme inventory (authenticated).")
public class UserSavedSchemesController {

    private final UserSavedSchemeService userSavedSchemeService;
    private final AppUserRepository appUserRepository;

    public UserSavedSchemesController(
            UserSavedSchemeService userSavedSchemeService, AppUserRepository appUserRepository) {
        this.userSavedSchemeService = userSavedSchemeService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    @Operation(summary = "List saved schemes", tags = {OpenApiTags.ACCOUNT})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not signed in", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<SavedSchemeItemDto>>> list(Authentication authentication) {
        AppUser user = AccountAuthSupport.requireAppUser(authentication, appUserRepository);
        return ResponseEntity.ok(ApiResponse.ok(userSavedSchemeService.listSaved(user)));
    }

    @PostMapping
    @Operation(
            summary = "Save a scheme",
            description = "Idempotent for the same user + scheme id (updates reminder preference).",
            tags = {OpenApiTags.ACCOUNT})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Saved row"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not signed in", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Unknown scheme id", content = @Content)
    })
    public ResponseEntity<ApiResponse<SavedSchemeItemDto>> save(
            Authentication authentication,
            @Valid @RequestBody SaveSchemeRequest body) {
        AppUser user = AccountAuthSupport.requireAppUser(authentication, appUserRepository);
        SavedSchemeItemDto dto = userSavedSchemeService.save(user, body);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @DeleteMapping("/{schemeId}")
    @Operation(summary = "Remove a saved scheme", tags = {OpenApiTags.ACCOUNT})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Removed (or was not saved)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not signed in", content = @Content)
    })
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String schemeId) {
        AppUser user = AccountAuthSupport.requireAppUser(authentication, appUserRepository);
        userSavedSchemeService.remove(user, schemeId);
        return ResponseEntity.noContent().build();
    }
}
