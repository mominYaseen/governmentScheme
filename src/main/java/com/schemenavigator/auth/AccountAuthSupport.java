package com.schemenavigator.auth;

import com.schemenavigator.model.AppUser;
import com.schemenavigator.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class AccountAuthSupport {

    private AccountAuthSupport() {
    }

    /**
     * Resolves {@link AppUser}: demo sessions use {@link DemoUserSessionPrincipal} (loaded from DB);
     * OAuth uses {@link AppUserPrincipal}.
     */
    public static AppUser requireAppUser(Authentication authentication, AppUserRepository appUserRepository) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        }
        Object p = authentication.getPrincipal();
        if (p instanceof DemoUserSessionPrincipal d) {
            return appUserRepository.findById(d.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required"));
        }
        if (p instanceof AppUser appUser) {
            return appUser;
        }
        if (p instanceof AppUserPrincipal ou) {
            return ou.getAppUser();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
    }
}
