package com.schemenavigator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI schemeNavigatorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Government Scheme Navigator API")
                        .version("1.0.0")
                        .description("""
                                Backend for discovering Indian government schemes from natural-language profiles.
                                Primary flow: POST /api/schemes/match with userInput (and optional language).
                                """))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local")));
    }
}
