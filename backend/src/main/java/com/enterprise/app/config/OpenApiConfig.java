package com.enterprise.app.config;

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
 * Configuration de la DOCUMENTATION OPENAPI (Swagger UI).
 *
 * <p><b>C'est quoi OpenAPI / Swagger ?</b></p>
 * <ul>
 *   <li><b>OpenAPI</b> est un standard de description d'une API REST : un gros
 *       document JSON (servi ici sur <a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a>)
 *       qui liste chaque endpoint, ses paramètres, ses réponses possibles et
 *       la forme des objets échangés (les "schémas").</li>
 *   <li><b>Swagger UI</b> est la page web lisible générée depuis ce document :
 *       <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a>.
 *       Elle permet de LIRE la doc ET de TESTER chaque endpoint depuis le
 *       navigateur ("Try it out").</li>
 * </ul>
 *
 * <p><b>D'où vient la documentation ?</b> De DEUX sources combinées :</p>
 * <ol>
 *   <li>Cette classe : les méta-données GLOBALES (titre, description, contact,
 *       serveurs, schéma de sécurité, groupes de tags) ;</li>
 *   <li>Les annotations dans les contrôleurs et DTO : le détail de CHAQUE
 *       endpoint ({@code @Operation}, {@code @Parameter}, {@code @ApiResponse})
 *       et de chaque champ ({@code @Schema}) — voir AuthController,
 *       UserController et les records de dto/.</li>
 * </ol>
 *
 * <p>Cette classe ajoute aussi le bouton <b>"Authorize"</b> de Swagger UI :
 * collez-y votre token JWT (SANS le mot "Bearer") et Swagger l'ajoutera
 * automatiquement à l'en-tête de chaque requête testée — indispensable pour
 * essayer /api/auth/me ou /api/users depuis la page.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Nom interne du schéma de sécurité (référencé par le bouton Authorize). */
    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI enterpriseOpenAPI() {
        return new OpenAPI()
                // -----------------------------------------------------------------
                // Bandeau affiché en haut de Swagger UI : titre, résumé, contact.
                // La description explique le DÉMARRAGE à un nouveau consommateur :
                // authentification, rôles, codes d'erreur, conventions.
                // (Markdown supporté : liens, listes, blocs de code.)
                // -----------------------------------------------------------------
                .info(new Info()
                        .title("Enterprise API — Gestion de projets")
                        .description("""
                                API REST du backend **enterprise-springboot-angular** : \
                                authentification JWT, administration des comptes utilisateurs. \
                                (Phase 2b : projets, tâches, commentaires.)

                                ## Démarrage rapide
                                1. **Créer un compte** : `POST /api/auth/register` (rôle USER attribué).
                                2. **Se connecter** : `POST /api/auth/login` → copier le champ `token`.
                                3. **S'authentifier** : bouton **Authorize** (en haut à droite),
                                   coller le token SANS le mot "Bearer", puis "Try it out" sur
                                   n'importe quel endpoint.
                                4. Compte ADMIN de démonstration : `admin@enterprise.com` / `Admin123!`
                                   (créé au 1er démarrage — à changer hors développement !).

                                ## Authentification
                                - L'API est **stateless** : pas de session, pas de cookie.
                                - Chaque requête protégée doit porter l'en-tête
                                  `Authorization: Bearer <token>`.
                                - Le token est un JWT signé **HS256**, valable 24 h
                                  (claim `id`, `email`, `fullName`, `roles`).

                                ## Rôles
                                | Rôle | Accès |
                                |---|---|
                                | `USER` | endpoints "authenticated" (ex. `/api/auth/me`) |
                                | `MANAGER` | + gestion des projets de son équipe (phase 2b) |
                                | `ADMIN` | + administration des comptes `/api/users/**` |

                                ## Erreurs (RFC 7807)
                                Toutes les erreurs renvoient un JSON "Problem Detail" :
                                `{ "type", "title", "status", "detail", "timestamp" }`.
                                En cas d'erreur de **validation**, une carte `errors`
                                supplémentaire liste champ par champ le motif du refus.

                                ## Conventions
                                - Pagination : `?page=0&size=20` (page = index 0-based),
                                  réponse `{ content[], totalElements, totalPages, number, size }`.
                                - Dates au format **ISO 8601 UTC** (`2026-09-28T14:30:00Z`).
                                - Les entités JPA ne sortent JAMAIS : uniquement des DTO.
                                """)
                        .version("v0.1.0")
                        .contact(new Contact()
                                .name("Équipe Enterprise")
                                .email("equipe@enterprise.example.com")
                                .url("https://github.com/example/enterprise-springboot-angular"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                // -----------------------------------------------------------------
                // Serveurs : les URL "où essayer l'API" proposées par Swagger UI.
                // (Défaut : l'origine courante de la page, ex. http://localhost:8080.)
                // -----------------------------------------------------------------
                .servers(List.of(
                        new Server().url("/").description("Serveur courant (dev local)"),
                        new Server().url("http://localhost:8080").description("Backend direct (port 8080)"),
                        new Server().url("http://localhost:80").description("Via nginx (Docker, proxifie /api)")))
                // -----------------------------------------------------------------
                // Sécurité : schéma HTTP Bearer + JWT → bouton "Authorize" global.
                // .addSecurityItem l'applique à TOUTE l'API ; les endpoints publics
                // (register, login) précisent security = {} dans leurs @Operation
                // pour signaler qu'ils n'en ont PAS besoin.
                // -----------------------------------------------------------------
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Collez le token renvoyé par POST /api/auth/login "
                                        + "SANS le préfixe 'Bearer'.")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
