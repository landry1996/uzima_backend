package com.uzima.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI 3 / Swagger UI.
 * <p>
 * Accessible via :
 * - Swagger UI  : /swagger-ui.html
 * - JSON spec   : /v3/api-docs
 * - YAML spec   : /v3/api-docs.yaml
 * <p>
 * Authentification : Bearer JWT (header Authorization: Bearer <token>)
 */
@Configuration
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI uzimaOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Développement local"),
                        new Server().url("https://api.uzima.app").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Uzima API")
                .version("1.0.0")
                .description("""
                        **Uzima Super-App** — API REST complète.

                        ## Domaines couverts
                        - **Auth** : inscription, connexion, refresh token JWT
                        - **Messages** : messagerie 7 types, IA (transcription, traduction, intention, émotion, résumé)
                        - **QR Codes** : 6 types contextuels, géofencing, personnalisation, révocation
                        - **Paiements** : envoi, demande, historique, annulation
                        - **Factures** : création, envoi, paiement, annulation
                        - **Workspace** : projets Kanban, tâches, time tracking
                        - **Social** : Cercles de Vie (Famille, Travail, Amis, Projets)
                        - **Assistant IA** : rappels contextuels (PENDING→TRIGGERED→SNOOZED→DISMISSED)
                        - **Bien-être** : sessions Focus, suivi usage apps, rapport de santé digitale

                        ## Authentification
                        La plupart des endpoints requièrent un **Bearer JWT** dans le header :
                        ```
                        Authorization: Bearer <access_token>
                        ```
                        Obtenez un token via `POST /api/auth/login`.
                        """)
                .contact(new Contact()
                        .name("Équipe Uzima")
                        .email("dev@uzima.app")
                )
                .license(new License()
                        .name("Propriétaire — © 2026 Uzima")
                );
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token JWT obtenu via POST /api/auth/login");
    }
}
