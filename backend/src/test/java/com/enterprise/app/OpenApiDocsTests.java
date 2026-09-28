package com.enterprise.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTS DU CONTRAT OPENAPI — la documentation EST une interface publique :
 * ces tests vérifient que /v3/api-docs expose bien tout ce que Swagger UI
 * affiche (métadonnées globales, endpoints, schémas, sécurité).
 *
 * <p>Le document est du JSON : on lit le corps brut en String et on utilise
 * des expressions régulières (souvent suffisantes pour un contrat) plutôt que
 * d'ajouter une dépendance de parsing JSON dédiée.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OpenAPI — contrat /v3/api-docs")
class OpenApiDocsTests {

    @Autowired private MockMvc mockMvc;

    /** Document OpenAPI brut, chargé une fois pour tous les tests. */
    private String docs;

    /** Charge (une fois) le document et vérifie qu'il est servi en 200. */
    private String docs() throws Exception {
        if (docs == null) {
            docs = mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }
        return docs;
    }

    /** Petit outil : vérifie qu'un fragment JSON (échappé pour regex) existe. */
    private void assertDocContains(String expectedFragment) throws Exception {
        String escaped = java.util.regex.Pattern.quote(expectedFragment);
        assertThat(Pattern.compile(escaped).matcher(docs()).find())
                .as("Le contrat OpenAPI doit contenir : %s", expectedFragment)
                .isTrue();
    }

    // ------------------------------------------------------------------
    // Métadonnées globales (OpenApiConfig)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le document OpenAPI est servi et décrit l'API")
    void openApiDocIsServed() throws Exception {
        assertThat(docs()).contains("\"openapi\"");
        // Métadonnées globales issues d'OpenApiConfig :
        assertDocContains("Enterprise API — Gestion de projets");
        assertDocContains("Démarrage rapide");
        assertDocContains("Équipe Enterprise");
    }

    @Test
    @DisplayName("le schéma de sécurité bearerAuth est déclaré")
    void securitySchemeIsDeclared() throws Exception {
        assertDocContains("bearerAuth");
        assertDocContains("bearer");
        assertDocContains("JWT");
    }

    @Test
    @DisplayName("tous les endpoints attendus sont documentés")
    void allPathsAreDocumented() throws Exception {
        assertDocContains("/api/auth/register");
        assertDocContains("/api/auth/login");
        assertDocContains("/api/auth/me");
        assertDocContains("/api/users");
        assertDocContains("/api/users/{id}");
        assertDocContains("/api/users/{id}/roles");
        assertDocContains("/api/users/{id}/enabled");
    }

    // ------------------------------------------------------------------
    // Documentation de détail (annotations @Operation / @ApiResponse)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("les opérations portent résumé et description enrichis")
    void operationsAreRichlyDocumented() throws Exception {
        // Résumés (une ligne par endpoint) :
        assertDocContains("Créer un compte (rôle USER)");
        assertDocContains("Se connecter et récupérer un token JWT");
        assertDocContains("Profil de l'utilisateur courant (JWT requis)");
        assertDocContains("Lister les utilisateurs (paginé)");
        assertDocContains("Modifier les rôles d'un utilisateur");
        assertDocContains("Activer ou désactiver un compte");
        assertDocContains("Supprimer un compte");

        // Descriptions (le "pourquoi/comment", en Markdown) :
        assertDocContains("BCrypt");
        assertDocContains("Pagination côté serveur");
        assertDocContains("HS256");
    }

    @Test
    @DisplayName("les codes d'erreur documentés couvrent la réalité HTTP")
    void errorResponsesAreDocumented() throws Exception {
        // 401/403/404 documentés sur les endpoints protégés :
        assertDocContains("\"401\"");
        assertDocContains("\"403\"");
        assertDocContains("\"404\"");
        assertDocContains("\"409\""); // email déjà pris (register)
        assertDocContains("\"204\""); // suppression sans contenu
    }

    @Test
    @DisplayName("les schémas des DTO sont exposés avec leurs exemples")
    void dtoSchemasAreExposed() throws Exception {
        assertDocContains("\"LoginResponse\"");
        assertDocContains("\"RegisterRequest\"");
        assertDocContains("\"LoginRequest\"");
        assertDocContains("\"UserResponse\"");
        assertDocContains("\"UpdateRolesRequest\"");
        assertDocContains("\"UpdateEnabledRequest\"");
        assertDocContains("\"UserPrincipal\"");

        // Exemples concrets (@Schema(example=...)) :
        assertDocContains("jane.doe@example.com");
        assertDocContains("Password123!");
    }

    @Test
    @DisplayName("les paramètres page/size sont documentés avec bornes")
    void paginationParamsAreDocumented() throws Exception {
        assertDocContains("Index de page (0 = première)");
        assertDocContains("Nombre d'utilisateurs par page (1\\u2013100)"
                .replace("\\u2013", "\u2013"));
    }

    @Test
    @DisplayName("les tags regroupent les endpoints par domaine")
    void tagsAreOrganized() throws Exception {
        assertDocContains("Authentification");
        assertDocContains("Utilisateurs");
    }

    // ------------------------------------------------------------------
    // Swagger UI
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Swagger UI est servie (page interactive)")
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                // Spring redirige vers /swagger-ui/index.html → 200 attendu
                // après suivi de redirection.
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus())
                                .isIn(200, 302));
    }
}
