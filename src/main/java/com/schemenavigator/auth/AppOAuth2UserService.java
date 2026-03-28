package com.schemenavigator.auth;

import com.schemenavigator.model.AppUser;
import com.schemenavigator.service.AppUserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserAccountService appUserAccountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AppUser appUser = appUserAccountService.registerOrUpdateFromOAuth(registrationId, oauthUser);
        return new AppUserPrincipal(appUser, oauthUser);
    }
}
