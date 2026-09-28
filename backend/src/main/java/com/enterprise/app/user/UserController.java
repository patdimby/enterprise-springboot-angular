package com.enterprise.app.user;

import com.enterprise.app.auth.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
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
 * ces endpoints reçoit un 403 Forbidden (testé dans les tests fonctionnels).
 * La documentation Swagger détaille, pour chaque endpoint, les paramètres
 * attendus, la forme de la réponse et TOUS les codes d'erreur possibles.</p>
 */
@RestController        // @Controller + toutes les réponses au format JSON
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Utilisateurs",
        description = "Administration des comptes — réservée au rôle ADMIN. "
                + "Cliquez sur 'Authorize' et connectez-vous avec un compte admin "
                + "(démo : admin@enterprise.com / Admin123!) avant de tester.")
@RequiredArgsConstructor  // Lombok : constructeur généré (injection de userService)
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users?page=0&size=20 — liste paginée.
     * {@code @RequestParam} récupère les paramètres d'URL "?page=0&size=20".
     */
    @GetMapping
    @Operation(
            summary = "Lister les utilisateurs (paginé)",
            description = """
                    Renvoie une **page** de comptes, triés par id croissant.

                    **Pagination côté serveur :** `page` (index 0-based) et `size`
                    contrôlent la fenêtre renvoyée — la base ne renvoie jamais plus
                    de `size` lignes, quelle que soit la taille du catalogue.

                    **Réponse :** structure Spring `Page` réduite à l'essentiel :
                    - `content` : tableau de UserResponse ;
                    - `totalElements` : nombre total d'utilisateurs ;
                    - `totalPages`, `number` (page courante), `size`.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'utilisateurs",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PageSchema.class))),
            @ApiResponse(responseCode = "401", description = "Token absent/invalide — ProblemDetail"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant (USER ou MANAGER) — ProblemDetail")
    })
    public Page<UserResponse> list(
            @Parameter(description = "Index de page (0 = première)", example = "0",
                    in = ParameterIn.QUERY, schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'utilisateurs par page (1–100)", example = "20",
                    in = ParameterIn.QUERY, schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size) {
        return userService.findPage(page, size);
    }

    /**
     * GET /api/users/5 — détail de l'utilisateur d'id 5.
     * {@code @PathVariable} récupère le "5" dans l'URL.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'un utilisateur",
            description = "Renvoie le compte demandé (DTO public : jamais de hash de mot de passe).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouvé — corps : UserResponse",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token absent/invalide — ProblemDetail"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant — ProblemDetail"),
            @ApiResponse(responseCode = "404", description = "Aucun utilisateur avec cet id — ProblemDetail")
    })
    public UserResponse get(
            @Parameter(description = "Identifiant technique du compte", example = "5",
                    in = ParameterIn.PATH, required = true,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", minimum = "1"))
            @PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * PATCH /api/users/5/roles — change les rôles (modification partielle).
     * {@code @Valid} déclenche la validation du JSON reçu (liste non vide).
     */
    @PatchMapping(value = "/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Modifier les rôles d'un utilisateur",
            description = """
                    **Remplace** la liste complète des rôles du compte
                    (modification partielle = PATCH, seule la propriété `roles` change).

                    **Valeurs possibles :** `ADMIN`, `MANAGER`, `USER`.

                    **Effets :** les nouveaux rôles sont pris en compte à la PROCHAINE
                    connexion (les rôles sont copiés dans le JWT à l'émission).

                    Garde-fous : liste vide → 400 (@NotEmpty) ; nom de rôle inconnu → 400.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôles mis à jour — corps : UserResponse",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Liste vide ou rôle inconnu — ProblemDetail"),
            @ApiResponse(responseCode = "401", description = "Token absent/invalide — ProblemDetail"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant — ProblemDetail"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable — ProblemDetail")
    })
    public UserResponse updateRoles(
            @Parameter(description = "Identifiant du compte à modifier", example = "5",
                    in = ParameterIn.PATH, required = true,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", minimum = "1"))
            @PathVariable Long id,
            @Parameter(description = "Nouvelle liste complète des rôles", required = true)
            @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateRoles(id, request.roles());
    }

    /**
     * PATCH /api/users/5/enabled — active/désactive le compte.
     * JSON attendu : {"enabled": false}
     */
    @PatchMapping(value = "/{id}/enabled", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Activer ou désactiver un compte",
            description = """
                    Bascule le drapeau `enabled` du compte :

                    - `false` : le compte **ne peut plus se connecter**
                      (AppUserDetailsService refuse les comptes désactivés) —
                      les tokens déjà émis restent néanmoins valables jusqu'à
                      expiration (JWT stateless) ;
                    - `true` : le compte peut de nouveau se connecter.

                    Alternative "douce" à la suppression : on conserve l'historique
                    du compte tout en bloquant l'accès.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "État mis à jour — corps : UserResponse",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Champ enabled absent — ProblemDetail"),
            @ApiResponse(responseCode = "401", description = "Token absent/invalide — ProblemDetail"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant — ProblemDetail"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable — ProblemDetail")
    })
    public UserResponse updateEnabled(
            @Parameter(description = "Identifiant du compte à modifier", example = "5",
                    in = ParameterIn.PATH, required = true,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", minimum = "1"))
            @PathVariable Long id,
            @Parameter(description = "Nouvel état du compte", required = true)
            @Valid @RequestBody UpdateEnabledRequest request) {
        return userService.updateEnabled(id, request.enabled());
    }

    /**
     * DELETE /api/users/5 — supprime le compte.
     * Réponse 204 No Content : "supprimé, rien à renvoyer".
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Supprimer un compte",
            description = """
                    Supprime **définitivement** le compte (et ses rôles, via la
                    table de jointure users_roles).

                    **204 No Content** : succès sans corps de réponse.

                    Irréversible — pour un blocage temporaire, préférez
                    `PATCH /api/users/{id}/enabled` avec `{"enabled": false}`.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Compte supprimé (aucun corps de réponse)"),
            @ApiResponse(responseCode = "401", description = "Token absent/invalide — ProblemDetail"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant — ProblemDetail"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable — ProblemDetail")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant du compte à supprimer", example = "5",
                    in = ParameterIn.PATH, required = true,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", minimum = "1"))
            @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Schéma dédié à la documentation de la réponse paginée.
     * <p>Spring sérialise {@code Page<T>} avec des champs techniques
     * (pageable, sort, first...) : cette classe décrit dans Swagger les seuls
     * champs utiles au consommateur, avec UserResponse comme type des éléments.
     * Elle ne sert QU'À la documentation — jamais sérialisée elle-même.</p>
     *
     * <p>Astuce : {@code @Schema} posé sur un TYPE (et non un champ) documente
     * une classe ; les champs d'exemple viennent des annotations @Schema
     * déjà présentes sur UserResponse.</p>
     */
    @Schema(name = "PageUserResponse",
            description = "Page d'utilisateurs (pagination Spring). `content` : les "
                    + "comptes de la page courante (tableau de UserResponse) ; "
                    + "`totalElements` : nombre total d'utilisateurs ; `totalPages` : "
                    + "nombre de pages ; `number` : index de la page (0-based) ; "
                    + "`size` : taille de la page.",
            example = ""
                    + "{ \"content\": [ { \"id\": 1, \"email\": \"admin@enterprise.com\", "
                    + "\"fullName\": \"Administrateur\", \"enabled\": true, "
                    + "\"roles\": [\"ADMIN\"], \"createdAt\": \"2026-01-01T00:00:00Z\" } ], "
                    + "\"totalElements\": 42, \"totalPages\": 3, \"number\": 0, \"size\": 20 }")
    private static class PageSchema {
    }
}
