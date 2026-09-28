package com.enterprise.app.user;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Requête de modification des rôles d'un utilisateur.
 * JSON attendu : {@code {"roles": ["USER", "MANAGER"]}}
 *
 * <p>{@code @NotEmpty} = la liste ne doit être ni absente ni vide :
 * pour vider tous les rôles d'un compte, passez par la désactivation.</p>
 */
public record UpdateRolesRequest(
        @NotEmpty List<String> roles
) {
}
