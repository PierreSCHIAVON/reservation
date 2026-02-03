package com.example.reservation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuration OpenAPI pour exposer la spécification openapi.yml.
 *
 * Cette configuration charge le fichier openapi.yml situé dans src/main/resources
 * et l'expose via Swagger UI à l'URL /swagger-ui.html
 *
 * Le fichier openapi.yml est la source de vérité pour l'API.
 * Les DTOs sont générés automatiquement depuis ce fichier via OpenAPI Generator.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() throws IOException {
        // Charger le fichier openapi.yml depuis les resources
        ClassPathResource resource = new ClassPathResource("openapi.yml");
        String yamlContent = resource.getContentAsString(StandardCharsets.UTF_8);

        // Parser le contenu YAML en objet OpenAPI
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(yamlContent, null, parseOptions).getOpenAPI();

        if (openAPI == null) {
            throw new IllegalStateException("Impossible de charger openapi.yml depuis le classpath");
        }

        return openAPI;
    }
}
