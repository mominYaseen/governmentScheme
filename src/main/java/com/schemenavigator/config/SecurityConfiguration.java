package com.schemenavigator.config;

import com.schemenavigator.auth.AppOAuth2UserService;
import com.schemenavigator.auth.DemoAuthenticationProvider;
import com.schemenavigator.auth.JsonAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final ObjectProvider<AppOAuth2UserService> appOAuth2UserService;
    private final ObjectProvider<DemoAuthenticationProvider> demoAuthenticationProvider;
    private final AuthUiProperties authUiProperties;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        demoAuthenticationProvider.ifAvailable(http::authenticationProvider);

        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                    "/",
                                    "/error",
                                    "/api/health/**",
                                    "/api/schemes/**",
                                    "/api/auth/status",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**",
                                    "/v3/api-docs")
                            .permitAll();
                    if ("oauth2".equalsIgnoreCase(authUiProperties.getType())) {
                        auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                    }
                    if (!"oauth2".equalsIgnoreCase(authUiProperties.getType())) {
                        auth.requestMatchers("/api/auth/login", "/api/auth/logout").permitAll();
                    }
                    auth.requestMatchers("/api/me", "/api/me/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        jsonAuthenticationEntryPoint,
                        new AntPathRequestMatcher("/api/**")));

        if ("oauth2".equalsIgnoreCase(authUiProperties.getType())) {
            http.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(u -> u.userService(appOAuth2UserService.getObject()))
                    .successHandler((request, response, authentication) -> {
                        String base = authUiProperties.getFrontendUrl().replaceAll("/+$", "");
                        response.sendRedirect(base + "/auth/callback");
                    }));
        }

        http.logout(logout -> logout
                .logoutSuccessUrl(authUiProperties.getFrontendUrl().replaceAll("/+$", "") + "/"));

        return http.build();
    }
}
