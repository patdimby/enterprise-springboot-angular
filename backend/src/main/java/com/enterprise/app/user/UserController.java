package com.enterprise.app.user;

import com.enterprise.app.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * Contrôleur d'ADMINISTRATION des utilisateurs — la porte d'entrée HTTP
 * de toutes les opérations sur les comptes.
 *
 * <p><b>C'est quoi un contrôleur ?</b> La classe qui reçoit les requêtes HTTP
 * et renvoie des réponses. Chaque méthode = un endpoint. Ici :
 * GET /api/users, GET /api/users/{id}, PATCH /api/users/{id}/roles, etc.</p>
 *
 * <p><b>Sécurité</b> : {@code @PreAuthorize("hasRole('ADMIN')")} s'applique à
 * TOUTE la classe — seuls les admins passent. Un utilisateur simple qui appelle
 * ces endpoints reçoit un 403 Forbidden (testé dans EnterpriseApplicationTests).</p>
 */
@RestController        // @Controller + toutes les réponses au format JSON
@RequestMapping("/api/users")  // Préfixe commun à tous les endpoints de la classe
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Utilisateurs", description = "Administration des comptes (ADMIN)")  // Groupe Swagger
@RequiredArgsConstructor  // Lombok : constructeur généré (injection de userService)
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users?page=0&size=20 — liste paginée.
     * {@code @RequestParam} récupère les paramètres d'URL "?page=0&size=20"
     * (avec valeurs par défaut si absents).
     */
    @GetMapping
    @Operation(summary = "Lister les utilisateurs (paginé)")
    public Page<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.findPage(page, size);
    }

    /**
     * GET /api/users/5 — détail de l'utilisateur d'id 5.
     * {@code @PathVariable} récupère le "5" dans l'URL.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    public UserResponse get(@PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * PATCH /api/users/5/roles — change les rôles (modification partielle).
     * {@code @Valid} déclenche la validation du JSON reçu (liste non vide).
     */
    @PatchMapping("/{id}/roles")
    @Operation(summary = "Modifier les rôles d'un utilisateur")
    public UserResponse updateRoles(@PathVariable Long id,
                                    @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateRoles(id, request.roles());
    }

    /**
     * PATCH /api/users/5/enabled — active/désactive le compte.
     * JSON attendu : {"enabled": false}
     */
    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Activer ou désactiver un compte")
    public UserResponse updateEnabled(@PathVariable Long id,
                                      @Valid @RequestBody UpdateEnabledRequest request) {
        return userService.updateEnabled(id, request.enabled());
    }

    /**
     * DELETE /api/users/5 — supprime le compte.
     * Réponse 204 No Content : "supprimé, rien à renvoyer".
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un compte")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
