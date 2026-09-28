package com.enterprise.app;

import com.enterprise.app.auth.AuthService;
import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTS FONCTIONNELS (intégration HTTP) de l'API — la suite complète.
 *
 * <p>Chaque test envoie de VRAIES requêtes HTTP (via MockMvc) et vérifie le
 * code de statut + le JSON renvoyé, comme le ferait un client : c'est la
 * définition d'un test fonctionnel. La base est H2 en mémoire (profil
 * "test") ; les tests s'exécutent dans l'ordre déclaré pour construire un
 * petit scénario métier cohérent (inscription → usage → administration).</p>
 *
 * <p>Organisation :
 * <ul>
 *   <li>{@code HealthEndpoints} — supervision publique ;</li>
 *   <li>{@code PublicEndpoints} — validation des entrées ;</li>
 *   <li>{@code AuthFlow} — inscription, /me, protection JWT ;</li>
 *   <li>{@code AdminFlow} — gestion des comptes par l'ADMIN.</li>
 * </ul></p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // Un seul "monde" partagé entre les méthodes
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API — tests fonctionnels HTTP (scénario complet)")
class ApiIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;

    // ------------------------------------------------------------------
    // 1. Supervision (health) — publique
    // ------------------------------------------------------------------

    @org.junit.jupiter.api.Nested
    @DisplayName("Sondes de santé (/actuator)")
    class HealthEndpoints {

        @Test
        @Order(1)
        @DisplayName("GET /actuator/health répond UP sans authentification")
        void healthIsPublicAndUp() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @Order(2)
        @DisplayName("GET /actuator/health/liveness et /readiness répondent UP")
        void livenessAndReadinessRespond() throws Exception {
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    // ------------------------------------------------------------------
    // 2. Endpoints publics : validation des entrées
    // ------------------------------------------------------------------

    @org.junit.jupiter.api.Nested
    @DisplayName("Validation des endpoints publics")
    class PublicEndpoints {

        @Test
        @Order(1)
        @DisplayName("POST /api/auth/register avec email invalide → 400 + détail champ par champ")
        void registerWithInvalidEmailIsRejected() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            // "pas-un-email" ne respecte pas @Email → 400.
                            .content("""
                                    {"email":"pas-un-email","password":"Password123!","fullName":"X"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    // GlobalExceptionHandler ajoute la carte "errors" :
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @Order(2)
        @DisplayName("POST /api/auth/register avec mot de passe trop court → 400")
        void registerWithShortPasswordIsRejected() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"short@example.com","password":"court","fullName":"X"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(3)
        @DisplayName("POST /api/auth/login avec JSON mal formé → 400")
        void loginWithMalformedJsonIsRejected() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{pas du json}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ------------------------------------------------------------------
    // 3. Flux d'authentification complet
    // ------------------------------------------------------------------

    @org.junit.jupiter.api.Nested
    @DisplayName("Flux d'authentification (register → login → me)")
    class AuthFlow {

        @Test
        @Order(1)
        @DisplayName("inscription → 201, puis login → JWT, puis /me → profil")
        void fullRegisterLoginMeFlow() throws Exception {
            // 1) Inscription : 201 CREATED.
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"flow@example.com","password":"Password123!","fullName":"Flow User"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("flow@example.com"))
                    .andExpect(jsonPath("$.roles[0]").value("USER"))
                    // Jamais de mot de passe (même hashé) dans une réponse !
                    .andExpect(jsonPath("$.password").doesNotExist());

            // 2) Login : 200 + token JWT exploitable.
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"flow@example.com","password":"Password123!"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andReturn();
            String token = readJson(loginResult, "token");

            // 3) /me avec le token : l'API identifie l'utilisateur.
            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("flow@example.com"));

            // 4) /me SANS token : 401 — le JWT protège bien la route.
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(2)
        @DisplayName("re-inscrire le même email → 409 Conflict")
        void duplicateRegisterIsConflicted() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"flow@example.com","password":"Password123!","fullName":"Clone"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @Order(3)
        @DisplayName("mauvais mot de passe au login → 401")
        void wrongPasswordIsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"flow@example.com","password":"Mauvais1x!"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(4)
        @DisplayName("login insensitive à la casse de l'email")
        void loginIsCaseInsensitiveOnEmail() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"FLOW@EXAMPLE.COM","password":"Password123!"}
                                    """))
                    .andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------------
    // 4. Administration des comptes (rôle ADMIN requis)
    // ------------------------------------------------------------------

    @org.junit.jupiter.api.Nested
    @DisplayName("Administration des comptes (/api/users, ADMIN)")
    class AdminFlow {

        @Test
        @Order(1)
        @DisplayName("GET /api/users sans authentification → 401, puis admin → 200 paginé")
        void listUsersRequiresAdmin() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @Order(2)
        @DisplayName("circuit complet admin : rôles, désactivation, détail, suppression")
        void fullAdminLifecycle() throws Exception {
            // On s'assure que le compte de test existe (inscription directe par le service).
            authService.register(new RegisterRequest("admin-life@example.com", "Password123!", "Life Admin"));
            UserResponse created = listByEmail("admin-life@example.com");

            // 1) Rôles : USER + MANAGER → 200 et rôles appliqués.
            mockMvc.perform(patch("/api/users/" + created.id() + "/roles")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roles":["USER","MANAGER"]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles.length()").value(2));

            // 2) Rôle inconnu → 400 (validé par UserService).
            mockMvc.perform(patch("/api/users/" + created.id() + "/roles")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roles":["PAS_UN_ROLE"]}
                                    """))
                    .andExpect(status().isBadRequest());

            // 3) Désactivation du compte → enabled=false.
            mockMvc.perform(patch("/api/users/" + created.id() + "/enabled")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"enabled":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));

            // 4) Détail du compte désactivé : toujours consultable.
            mockMvc.perform(get("/api/users/" + created.id()).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("admin-life@example.com"));

            // 5) Suppression → 204, puis le détail renvoie 404.
            mockMvc.perform(delete("/api/users/" + created.id()).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/users/" + created.id()).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(3)
        @DisplayName("rôle MANAGER : /api/users reste interdit (403) et /me autorisé")
        void managerCannotAdminister() throws Exception {
            // Le compte d'AdminFlow.order2 a été supprimé ; on en recrée un.
            authService.register(new RegisterRequest("manager@example.com", "Password123!", "Mgr"));
            UserResponse mgr = listByEmail("manager@example.com");
            // Promotion en MANAGER (via le service, pas besoin de HTTP ici).
            mockMvc.perform(patch("/api/users/" + mgr.id() + "/roles")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roles":["MANAGER"]}
                                    """))
                    .andExpect(status().isOk());

            LoginResponse login = authService.login(new LoginRequest("manager@example.com", "Password123!"));

            // /api/users reste réservé à l'ADMIN → 403.
            mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + login.token()))
                    .andExpect(status().isForbidden());

            // Mais l'utilisateur reste valide pour les routes "authenticated".
            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("manager@example.com"));
        }
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    /** Extrait une propriété JSON simple du corps d'une réponse (mini-parseur). */
    private String readJson(MvcResult result, String field) throws Exception {
        String body = result.getResponse().getContentAsString();
        // Suffisant pour des tests : extrait "field":"valeur" ou "field":123.
        var matcher = java.util.regex.Pattern
                .compile("\\\"" + field + "\\\"\\s*:\\s*(\\\\?\"|)([^\",}]+)\\1")
                .matcher(body);
        assertThat(matcher.find()).as("champ JSON '%s' présent dans %s", field, body).isTrue();
        return matcher.group(2);
    }

    /** Retrouve un utilisateur par email via la liste paginée admin. */
    private UserResponse listByEmail(String email) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/users?page=0&size=50").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        String body = page.getResponse().getContentAsString();
        // Parcourt les objets {..} du tableau "content" pour trouver l'email.
        var matcher = java.util.regex.Pattern
                .compile("\\{[^{}]*?\\\"" + java.util.regex.Pattern.quote(email) + "\\\"[^{}]*}")
                .matcher(body);
        assertThat(matcher.find()).as("utilisateur %s présent dans la liste", email).isTrue();
        String object = matcher.group();
        var idMatcher = java.util.regex.Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)").matcher(object);
        assertThat(idMatcher.find()).isTrue();
        long id = Long.parseLong(idMatcher.group(1));
        return new UserResponse(id, email, null, true, null, null);
    }
}
