package com.schemenavigator.service;

import com.schemenavigator.model.AppUser;
import com.schemenavigator.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppUserAccountService {

    private final AppUserRepository appUserRepository;

    /**
     * Fixed demo account ({@code app.auth.type=demo}); provider {@code demo}, subject = email.
     */
    @Transactional
    public AppUser getOrCreateDemoUser(String email) {
        String normalized = email.trim();
        return appUserRepository.findByProviderAndProviderSubject("demo", normalized)
                .orElseGet(() -> appUserRepository.save(AppUser.builder()
                        .provider("demo")
                        .providerSubject(normalized)
                        .email(normalized)
                        .displayName("Demo user")
                        .build()));
    }

    @Transactional
    public AppUser registerOrUpdateFromOAuth(String registrationId, OAuth2User oauthUser) {
        String subject = oauthUser.getName();
        if (subject == null || subject.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user", "Missing subject", null));
        }
        String provider = registrationId.toLowerCase(Locale.ROOT);
        String email = resolveEmail(oauthUser.getAttributes());
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user", "Email required — use openid profile email scopes", null));
        }
        String displayName = resolveDisplayName(oauthUser.getAttributes());

        return appUserRepository.findByProviderAndProviderSubject(provider, subject)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setDisplayName(displayName);
                    return appUserRepository.save(existing);
                })
                .orElseGet(() -> appUserRepository.save(AppUser.builder()
                        .provider(provider)
                        .providerSubject(subject)
                        .email(email)
                        .displayName(displayName)
                        .build()));
    }

    private static String resolveEmail(Map<String, Object> attributes) {
        if (attributes == null) {
            return null;
        }
        Object email = attributes.get("email");
        if (email != null) {
            return email.toString();
        }
        return null;
    }

    private static String resolveDisplayName(Map<String, Object> attributes) {
        if (attributes == null) {
            return null;
        }
        Object name = attributes.get("name");
        if (name != null) {
            return name.toString();
        }
        Object given = attributes.get("given_name");
        Object family = attributes.get("family_name");
        if (given != null || family != null) {
            return (given != null ? given.toString() : "") + " " + (family != null ? family.toString() : "");
        }
        return null;
    }
}
