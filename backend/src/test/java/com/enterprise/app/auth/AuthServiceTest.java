package com.enterprise.app.auth;

import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import com.enterprise.app.config.AppProperties;
import com.enterprise.app.security.JwtService;
import com.enterprise.app.user.Role;
import com.enterprise.app.user.RoleName;
import com.enterprise.app.user.RoleRepository;
import com.enterprise.app.user.User;
import com.enterprise.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TESTS UNITAIRES de {@link AuthService}.
 *
 * <p>On isole le service de ses dépendances grâce à <b>Mockito</b> : les
 * collaborateurs ({@code UserRepository}, {@code AuthenticationManager}...)
 * sont remplacés par des SIMULACRES ("mocks") dont on programme les
 * réponses avec {@code when(...)}. On teste ainsi UNIQUEMENT la logique du
 * service, sans base de données ni Spring — millisecondes comprises.</p>
 */
@ExtendWith(MockitoExtension.class)   // Branche Mockito dans JUnit 5
@DisplayName("AuthService — inscription et connexion (isolé, sans base)")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    /** Le service testé — construit à la main avec les mocks ci-dessus. */
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder,
                authenticationManager, jwtService);
    }

    // ------------------------------------------------------------------
    // Inscription
    // ------------------------------------------------------------------

    @Test
    @DisplayName("register crée le compte avec le rôle USER et un mot de passe hashé")
    void registerEncodesPasswordAndAssignsUserRole() {
        // --- Arrange : on programme le comportement des mocks -------------
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(Role.builder().name(RoleName.USER).build()));
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hash-simulé");
        // any(User.class) : peu importe l'entité reçue, la base "renvoie" un id 7.
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            u.setCreatedAt(Instant.now());
            return u;
        });

        // --- Act : le vrai appel au service -------------------------------
        UserResponse response = authService.register(
                new RegisterRequest("jane@example.com", "Password123!", "Jane Doe"));

        // --- Assert : le résultat ET les interactions ---------------------
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.roles()).containsExactly("USER");
        // Le mot de passe a bien transité par l'encodeur (jamais stocké en clair).
        verify(passwordEncoder).encode("Password123!");
    }

    @Test
    @DisplayName("register refuse un email déjà pris (409 Conflict)")
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("taken@example.com", "Password123!", "Twin")))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
    }

    // ------------------------------------------------------------------
    // Connexion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("login authentifie, puis émet un JWT avec les rôles de l'utilisateur")
    void loginReturnsSignedTokenAndRoles() {
        User jane = User.builder()
                .email("jane@example.com")
                .password("$2a$10$hash-simulé")
                .fullName("Jane Doe")
                .enabled(true)
                .roles(Set.of(Role.builder().name(RoleName.USER).build()))
                .build();
        jane.setId(3L);

        // L'AuthenticationManager "valide" le couple email/mot de passe.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(jane, null, List.of()));
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(jane));
        when(jwtService.generateToken(3L, "jane@example.com", "Jane Doe", List.of("USER")))
                .thenReturn("jwt-simulé");

        LoginResponse response = authService.login(new LoginRequest("jane@example.com", "Password123!"));

        assertThat(response.token()).isEqualTo("jwt-simulé");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    @DisplayName("login lève BadCredentialsException si les identifiants sont faux")
    void loginRejectsBadCredentials() {
        // Le gestionnaire d'authentification refuse → AuthenticationException.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("nope"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "Wrong1!x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login échoue si l'utilisateur authentifié a disparu entre-temps")
    void loginFailsWhenUserVanishes() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("ghost@example.com", null, List.of()));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "Password123!")))
                .isInstanceOf(IllegalStateException.class);
    }
}
