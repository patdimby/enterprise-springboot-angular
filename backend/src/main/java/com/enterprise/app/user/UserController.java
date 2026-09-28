package com.enterprise.app.user;

import com.enterprise.app.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration des utilisateurs (réservée au rôle ADMIN, contrôlé aussi par
 * la règle {@code /api/users/**} de SecurityConfig).
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Utilisateurs", description = "Administration des comptes (ADMIN)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs (paginé)")
    public Page<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.findPage(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    public UserResponse get(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Modifier les rôles d'un utilisateur")
    public UserResponse updateRoles(@PathVariable Long id,
                                    @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateRoles(id, request.roles());
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Activer ou désactiver un compte")
    public UserResponse updateEnabled(@PathVariable Long id,
                                      @Valid @RequestBody UpdateEnabledRequest request) {
        return userService.updateEnabled(id, request.enabled());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un compte")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
