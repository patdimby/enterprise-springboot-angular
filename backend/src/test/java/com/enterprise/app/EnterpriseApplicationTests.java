package com.enterprise.app;

import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import com.enterprise.app.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie le démarrage complet, le flux register/login et les règles d'accès.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnterpriseApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void contextLoads() {
    }

    @Test
    void registerAndLoginReturnsJwt() {
        RegisterRequest register = new RegisterRequest("jane.doe@example.com", "Password123!", "Jane Doe");
        UserResponse registered = authService.register(register);
        assertThat(registered.id()).isNotNull();
        assertThat(registered.roles()).containsExactly("USER");

        LoginResponse login = authService.login(new LoginRequest("jane.doe@example.com", "Password123!"));
        assertThat(login.token()).isNotBlank();
        assertThat(login.roles()).containsExactly("USER");
    }

    @Test
    void loginRejectsWrongPassword() {
        RegisterRequest register = new RegisterRequest("bob@example.com", "Password123!", "Bob");
        authService.register(register);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> authService.login(new LoginRequest("bob@example.com", "WrongPass1!")))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void usersEndpointRequiresAdminRole() throws Exception {
        // Sans token -> 401
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        // Token utilisateur simple (rôle USER) -> 403
        RegisterRequest register = new RegisterRequest("carol@example.com", "Password123!", "Carol");
        authService.register(register);
        LoginResponse userLogin = authService.login(new LoginRequest("carol@example.com", "Password123!"));
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + userLogin.token()))
                .andExpect(status().isForbidden());

        // Token ADMIN -> 200 avec contenu paginé
        LoginResponse adminLogin = authService.login(new LoginRequest("admin@enterprise.com", "Admin123!"));
        assertThat(adminLogin.roles()).containsExactly("ADMIN");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminLogin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }
}
