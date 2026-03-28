package com.schemenavigator.auth;

import com.schemenavigator.model.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Wraps the OAuth2 user and the persisted {@link AppUser}.
 */
@Getter
public class AppUserPrincipal implements OAuth2User {

    private final AppUser appUser;
    private final OAuth2User oauth2User;

    public AppUserPrincipal(AppUser appUser, OAuth2User oauth2User) {
        this.appUser = appUser;
        this.oauth2User = oauth2User;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities() != null
                ? oauth2User.getAuthorities()
                : Collections.emptyList();
    }

    @Override
    public String getName() {
        return appUser.getId().toString();
    }
}
