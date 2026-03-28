package com.schemenavigator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${openapi.public-url:}")
    private String publicUrl;

    @Bean
    public OpenAPI schemeNavigatorOpenApi() {
        Info info = new Info()
                .title("Government Scheme Navigator API")
                .version("1.0.0")
                .description("""
                        **Government Scheme Navigator** — REST API for listing schemes, rule-based recommendations (CSV-backed \
                        `eligibility_criteria`), and natural-language matching with Google Gemini.

                        ### Response envelope
                        Most JSON responses use `ApiResponse<T>`:
                        - `success` — `true` when `data` is set
                        - `data` — response payload (type varies by endpoint)
                        - `error` — human-readable message when `success` is `false`
                        - `timestamp` — ISO-8601 instant string

                        ### Which endpoint should the frontend use?
                        | Goal | Method | Path |
                        |------|--------|------|
                        | Paginated scheme catalog | GET | `/api/schemes` |
                        | One scheme (full text for UI summary) | GET | `/api/schemes/{id}` |
                        | Structured profile → eligible schemes (SQL rules) | POST | `/api/schemes/recommend` |
                        | Free-text profile → Gemini + legacy rule engine | POST | `/api/schemes/match` |
                        | Liveness | GET | `/api/health` |
                        | Demo login (when `app.auth.type=demo`) | POST | `/api/auth/login` |
                        | Demo logout | POST | `/api/auth/logout` |
                        | Start Google OAuth (when `app.auth.type=oauth2`) | GET | `/oauth2/authorization/google` |
                        | Current user (session) | GET | `/api/me` |
                        | Saved schemes | GET | `/api/me/saved-schemes` |
                        | Save scheme | POST | `/api/me/saved-schemes` |
                        | Remove saved | DELETE | `/api/me/saved-schemes/{schemeId}` |

                        ### Pagination (GET /api/schemes)
                        Standard Spring `Pageable` query params: `page` (0-based), `size`, `sort` (e.g. `sort=name,asc`).

                        ### OpenAPI groups (Swagger UI dropdown)
                        - **all** — every endpoint
                        - **catalog** — health + list + recommend (no Gemini)
                        - **ai** — natural-language match only
                        - **account** — demo login or OAuth2 + saved schemes

                        **Authentication:** catalog / AI endpoints stay **anonymous**. Account endpoints use a **session cookie** \
                        (`demo`: `POST /api/auth/login`; `oauth2`: Google). Mutating requests need header `X-XSRF-TOKEN` (from `XSRF-TOKEN` cookie).
                        """)
                .contact(new Contact()
                        .name("Scheme Navigator")
                        .url("https://github.com"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html"));

        OpenAPI openApi = new OpenAPI()
                .info(info)
                .tags(List.of(
                        new Tag().name(OpenApiTags.HEALTH).description("Service liveness for load balancers and dev checks."),
                        new Tag().name(OpenApiTags.SCHEMES_CATALOG).description(
                                "Read-only catalog and **rule-based** recommendations using `eligibility_criteria` "
                                        + "(imported from CSV). No LLM calls."),
                        new Tag().name(OpenApiTags.SCHEMES_AI).description(
                                "Natural-language flow: profile extraction + ranking + explanations via **Gemini** "
                                        + "and legacy `eligibility_rules` on seed schemes."),
                        new Tag().name(OpenApiTags.ACCOUNT).description(
                                "Demo email/password or OAuth2 (Google): profile, saved schemes, apply reminders by email.")))
                .components(new Components()
                        .schemas(Map.of(
                                "ErrorMessage", new Schema<String>()
                                        .type("string")
                                        .description("Validation or business error description")
                                        .example("User input cannot be empty"))));

        openApi.addServersItem(new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development"));

        if (publicUrl != null && !publicUrl.isBlank()) {
            String url = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            openApi.addServersItem(new Server().url(url).description("Deployed / public base URL"));
        }

        return openApi;
    }

    @Bean
    public GroupedOpenApi allEndpointsGroup() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("All endpoints")
                .pathsToMatch("/api/**", "/")
                .build();
    }

    @Bean
    public GroupedOpenApi catalogGroup() {
        return GroupedOpenApi.builder()
                .group("catalog")
                .displayName("Catalog & recommend (no AI)")
                .pathsToMatch("/api/health/**", "/api/schemes/**")
                .pathsToExclude("/api/schemes/match")
                .build();
    }

    @Bean
    public GroupedOpenApi aiMatchGroup() {
        return GroupedOpenApi.builder()
                .group("ai")
                .displayName("AI match (Gemini)")
                .pathsToMatch("/api/schemes/match")
                .build();
    }

    @Bean
    public GroupedOpenApi accountGroup() {
        return GroupedOpenApi.builder()
                .group("account")
                .displayName("Account & saved schemes")
                .pathsToMatch("/api/me/**", "/api/auth/**")
                .build();
    }
}
