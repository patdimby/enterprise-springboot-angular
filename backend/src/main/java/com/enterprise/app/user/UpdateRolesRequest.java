package com.enterprise.app.user;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Requête de modification des rôles d'un utilisateur.
 */
public record UpdateRolesRequest(
        @NotEmpty List<String> roles
) {
}
