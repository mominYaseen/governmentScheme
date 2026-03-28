package com.schemenavigator.config;

import com.schemenavigator.auth.DemoAuthenticationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "app.auth.type", havingValue = "demo", matchIfMissing = true)
public class DemoAuthConfiguration {

    @Bean
    public AuthenticationManager authenticationManager(DemoAuthenticationProvider demoAuthenticationProvider) {
        return new ProviderManager(List.of(demoAuthenticationProvider));
    }
}
