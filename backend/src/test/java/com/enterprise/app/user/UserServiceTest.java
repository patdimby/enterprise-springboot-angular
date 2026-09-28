package com.enterprise.app.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TESTS UNITAIRES de {@link UserService} — même principe que
 * {@code AuthServiceTest} : Mockito simule les repositories, on teste la
 * logique seule (pagination, 404, changement de rôles...).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — administration des comptes (isolé, sans base)")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository);
    }

    /** Petite fabrique d'utilisateur pour alléger les tests. */
    private User user(long id, String email, RoleName... roles) {
        User u = User.builder()
                .email(email)
                .password("$2a$10$hash")
                .fullName("Utilisateur " + id)
                .enabled(true)
                .roles(Set.of(Role.builder().name(roles[0]).build()))
                .build();
        u.setId(id);
        u.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
        return u;
    }

    @Test
    @DisplayName("findPage renvoie une page de DTOs (jamais d'entités)")
    void findPageMapsEntitiesToResponses() {
        User u1 = user(1L, "a@example.com", RoleName.USER);
        User u2 = user(2L, "b@example.com", RoleName.ADMIN);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(u1, u2)));

        Page<com.enterprise.app.auth.dto.UserResponse> page = userService.findPage(0, 20);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).email()).isEqualTo("a@example.com");
        assertThat(page.getContent().get(1).roles()).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("findById renvoie le DTO, ou 404 si l'id n'existe pas")
    void findByIdReturnsOrThrows404() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, "five@example.com", RoleName.USER)));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(userService.findById(5L).email()).isEqualTo("five@example.com");  // DTO com.enterprise.app.auth.dto.UserResponse

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("updateRoles remplace les rôles quand tous les noms existent")
    void updateRolesReplacesRoles() {
        User u = user(2L, "b@example.com", RoleName.USER);
        Role manager = Role.builder().name(RoleName.MANAGER).build();
        Role admin = Role.builder().name(RoleName.ADMIN).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.of(manager));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        com.enterprise.app.auth.dto.UserResponse response = userService.updateRoles(2L, List.of("MANAGER", "ADMIN"));

        assertThat(response.roles()).containsExactlyInAnyOrder("MANAGER", "ADMIN");
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("updateRoles refuse un nom de rôle inconnu (400) sans rien sauvegarder")
    void updateRolesRejectsUnknownRole() {
        User u = user(2L, "b@example.com", RoleName.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> userService.updateRoles(2L, List.of("SUPERGIRL")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        // Aucun save : la base reste intacte (l'erreur survient AVANT l'écriture).
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateEnabled change le drapeau et persiste")
    void updateEnabledPersistsFlag() {
        User u = user(4L, "d@example.com", RoleName.USER);
        when(userRepository.findById(4L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        com.enterprise.app.auth.dto.UserResponse response = userService.updateEnabled(4L, false);

        assertThat(response.enabled()).isFalse();
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("delete supprime si présent, ou 404 sinon")
    void deleteChecksExistence() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(404L)).thenReturn(false);

        userService.delete(1L);
        verify(userRepository).deleteById(1L);

        assertThatThrownBy(() -> userService.delete(404L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(userRepository, never()).deleteById(404L);
    }

    /** Vérification de la pagination demandée au repository (page 0, taille 20). */
    @Test
    @DisplayName("findPage demande bien la page demandée au repository")
    void findPageUsesRequestedPageable() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        userService.findPage(3, 50);
        verify(userRepository).findAll(PageRequest.of(3, 50, org.springframework.data.domain.Sort.by("id").ascending()));
    }
}
