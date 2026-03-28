package com.schemenavigator.controller;

import com.schemenavigator.dto.AccountProfileDto;
import com.schemenavigator.dto.DemoLoginRequest;
import com.schemenavigator.model.ApiResponse;
import com.schemenavigator.auth.DemoUserSessionPrincipal;
import com.schemenavigator.config.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.auth.type", havingValue = "demo", matchIfMissing = true)
@Tag(name = OpenApiTags.ACCOUNT, description = "Demo login (when app.auth.type=demo).")
public class DemoAuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public DemoAuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Demo login (email + password)",
            description = "Creates a session cookie. Use the same CSRF rules as other POSTs.",
            tags = {OpenApiTags.ACCOUNT})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged in"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad credentials", content = @Content)
    })
    public ResponseEntity<ApiResponse<AccountProfileDto>> login(
            @Valid @RequestBody DemoLoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated(body.email().trim(), body.password());
        org.springframework.security.core.Authentication auth = authenticationManager.authenticate(token);
        request.getSession(true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        DemoUserSessionPrincipal u = (DemoUserSessionPrincipal) auth.getPrincipal();
        AccountProfileDto dto = new AccountProfileDto(
                u.id(), u.email(), u.displayName(), u.provider());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "End session", tags = {OpenApiTags.ACCOUNT})
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }
}
