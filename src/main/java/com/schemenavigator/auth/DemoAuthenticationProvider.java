package com.schemenavigator.auth;

import com.schemenavigator.config.AuthUiProperties;
import com.schemenavigator.service.AppUserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auth.type", havingValue = "demo", matchIfMissing = true)
public class DemoAuthenticationProvider implements AuthenticationProvider {

    private final AuthUiProperties authUiProperties;
    private final AppUserAccountService appUserAccountService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Email required");
        }
        String normalized = email.trim();
        if (!authUiProperties.getDemoEmail().trim().equalsIgnoreCase(normalized)
                || !authUiProperties.getDemoPassword().equals(password)) {
            throw new BadCredentialsException("Invalid email or password");
        }
        var user = appUserAccountService.getOrCreateDemoUser(normalized);
        DemoUserSessionPrincipal principal = new DemoUserSessionPrincipal(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getProvider());
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
