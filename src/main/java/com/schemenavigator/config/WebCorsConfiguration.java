package com.schemenavigator.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class WebCorsConfiguration {

    private final AuthUiProperties authUiProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = expandOrigins(authUiProperties.getCorsAllowedOrigins());
        if (origins == null || origins.isEmpty()) {
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
        } else {
            config.setAllowedOrigins(origins);
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Split comma-separated entries so env like {@code APP_CORS_ORIGINS=a,b} or YAML lists work. */
    private static List<String> expandOrigins(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw) {
            if (part == null || part.isBlank()) {
                continue;
            }
            Arrays.stream(part.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(out::add);
        }
        return new ArrayList<>(out);
    }
}
