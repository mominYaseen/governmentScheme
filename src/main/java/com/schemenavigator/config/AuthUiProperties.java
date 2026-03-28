package com.schemenavigator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public class AuthUiProperties {

    /**
     * {@code demo} — fixed email/password and {@code POST /api/auth/login}.<br>
     * {@code oauth2} — Google (and optional GitHub) via {@code /oauth2/authorization/*}.
     */
    private String type = "demo";

    /** Used when {@code app.auth.type=demo}. */
    private String demoEmail = "demo@example.com";

    /** Used when {@code app.auth.type=demo}. */
    private String demoPassword = "demo123";

    /**
     * Where the browser is sent after successful OAuth (e.g. http://localhost:5173/auth/callback).
     */
    private String frontendUrl = "http://localhost:5173";

    /**
     * Allowed origins for CORS (session cookies + OAuth redirect). Include every SPA URL you use
     * (e.g. both localhost and 127.0.0.1 if you switch between them).
     */
    private List<String> corsAllowedOrigins = new ArrayList<>(
            List.of("http://localhost:5173", "http://127.0.0.1:5173"));

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDemoEmail() {
        return demoEmail;
    }

    public void setDemoEmail(String demoEmail) {
        this.demoEmail = demoEmail;
    }

    public String getDemoPassword() {
        return demoPassword;
    }

    public void setDemoPassword(String demoPassword) {
        this.demoPassword = demoPassword;
    }
}
