package com.enterprise.app;

import com.enterprise.app.auth.AuthService;
import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTS D'INTÉGRATION — ils démarrent la VRAIE application (mais avec la
 * base H2 en mémoire du profil "test", pas MySQL).
 *
 * <p>Les annotations au-dessus de la classe :</p>
 * <ul>
 *   <li>{@code @SpringBootTest} : "démarre toute l'application pour les tests" ;</li>
 *   <li>{@code @AutoConfigureMockMvc} : fournit MockMvc, un faux navigateur
 *       pour envoyer des requêtes HTTP sans démarrer de vrai serveur ;</li>
 *   <li>{@code @ActiveProfiles("test")} : utilise application-test.yml
 *       (H2 en mémoire, tokens courts...).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnterpriseApplicationTests {

    /** L'objet qui envoie les requêtes HTTP de test. */
    @Autowired
    private MockMvc mockMvc;

    /** Le service d'authentification réel, injecté par Spring. */
    @Autowired
    private AuthService authService;

    /** Test 1 : l'application démarre sans erreur (le contexte Spring se charge). */
    @Test
    void contextLoads() {
    }

    /**
     * Test 2 : le flux complet inscription → connexion.
     * On vérifie que le compte créé a le rôle USER et que le login
     * renvoie un token JWT non vide.
     */
    @Test
    void registerAndLoginReturnsJwt() {
        // Inscription d'un nouvel utilisateur.
        RegisterRequest register = new RegisterRequest("jane.doe@example.com", "Password123!", "Jane Doe");
        UserResponse registered = authService.register(register);
        assertThat(registered.id()).isNotNull();          // Un id a été généré par la base
        assertThat(registered.roles()).containsExactly("USER"); // Rôle par défaut = USER

        // Connexion avec les mêmes identifiants → token JWT.
        LoginResponse login = authService.login(new LoginRequest("jane.doe@example.com", "Password123!"));
        assertThat(login.token()).isNotBlank();           // Un token a bien été émis
        assertThat(login.roles()).containsExactly("USER");
    }

    /** Test 3 : un mauvais mot de passe est refusé (exception BadCredentialsException). */
    @Test
    void loginRejectsWrongPassword() {
        // On crée le compte bob@example.com.
        RegisterRequest register = new RegisterRequest("bob@example.com", "Password123!", "Bob");
        authService.register(register);

        // Tentative de connexion avec un mauvais mot de passe → exception.
        assertThatThrownBy(
                        () -> authService.login(new LoginRequest("bob@example.com", "WrongPass1!")))
                .isInstanceOf(BadCredentialsException.class);
    }

    /**
     * Test 4 : les règles d'accès HTTP (qui peut appeler /api/users ?).
     * <ul>
     *   <li>sans token → 401 Unauthorized ;</li>
     *   <li>avec un token USER simple → 403 Forbidden (pas admin) ;</li>
     *   <li>avec le token ADMIN (compte seedé par DataInitializer) → 200 OK
     *       avec une réponse paginée (champ "content").</li>
     * </ul>
     */
    @Test
    void usersEndpointRequiresAdminRole() throws Exception {
        // Cas 1 : aucun token → 401.
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        // Cas 2 : token d'un utilisateur simple (rôle USER) → 403.
        RegisterRequest register = new RegisterRequest("carol@example.com", "Password123!", "Carol");
        authService.register(register);
        LoginResponse userLogin = authService.login(new LoginRequest("carol@example.com", "Password123!"));
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + userLogin.token()))
                .andExpect(status().isForbidden());

        // Cas 3 : token ADMIN (créé automatiquement au démarrage) → 200 + données.
        LoginResponse adminLogin = authService.login(new LoginRequest("admin@enterprise.com", "Admin123!"));
        assertThat(adminLogin.roles()).containsExactly("ADMIN");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminLogin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists()); // $.content = la liste paginée
    }
}
