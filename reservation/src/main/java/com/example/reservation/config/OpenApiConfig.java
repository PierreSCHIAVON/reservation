package com.example.reservation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reservation API")
                        .description("""
                                API de gestion de réservations de propriétés.

                                ## Authentification

                                La plupart des endpoints nécessitent un token JWT valide obtenu via Keycloak.

                                Pour obtenir un token de test:
                                ```bash
                                curl -X POST 'http://localhost:8081/realms/reservation/protocol/openid-connect/token' \\
                                  -H 'Content-Type: application/x-www-form-urlencoded' \\
                                  -d 'grant_type=password&client_id=reservation-test&username=testuser&password=testuser'
                                ```

                                ## Endpoints publics

                                - `GET /api/properties` - Liste des propriétés actives
                                - `GET /api/properties/{id}` - Détails d'une propriété
                                - `GET /actuator/health` - Vérification de santé
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenu via Keycloak")));
    }
}
