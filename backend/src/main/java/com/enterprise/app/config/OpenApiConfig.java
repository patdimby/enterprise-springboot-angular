package com.enterprise.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la DOCUMENTATION Swagger UI.
 *
 * <p><b>C'est quoi Swagger ?</b> Une page web qui liste tous les endpoints de
 * l'API et permet de les tester depuis le navigateur :
 * http://localhost:8080/swagger-ui.html
 * La documentation est GÉNÉRÉE automatiquement depuis le code (annotations
 * @Tag, @Operation sur les contrôleurs).</p>
 *
 * <p>Cette classe ajoute surtout le bouton "Authorize" : collez-y votre token
 * JWT (sans le mot "Bearer") et Swagger l'ajoutera à chaque requête testée.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Nom interne du schéma de sécurité (référencé dans le bouton Authorize). */
    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI enterpriseOpenAPI() {
        return new OpenAPI()
                // Bandeau titre/description/version affiché en haut de Swagger UI.
                .info(new Info()
                        .title("Enterprise API")
                        .description("API REST de gestion de projets — Spring Boot 4, JWT, MySQL")
                        .version("v0.1.0")
                        .contact(new Contact().name("Équipe Enterprise")))
                // Déclare le type d'authentification : HTTP Bearer avec JWT.
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Applique ce schéma à toute l'API → bouton "Authorize" global.
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
