package com.enterprise.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Requête de modification des rôles d'un utilisateur.
 * JSON attendu : {@code {"roles": ["USER", "MANAGER"]}}
 *
 * <p>{@code @NotEmpty} = la liste ne doit être ni absente ni vide :
 * pour vider tous les rôles d'un compte, passez par la désactivation.</p>
 */
@Schema(description = "Nouvelle liste COMPLÈTE des rôles du compte "
        + "(PATCH /api/users/{id}/roles). Les valeurs possibles sont "
        + "ADMIN, MANAGER et USER ; tout autre nom est refusé (400).")
public record UpdateRolesRequest(
        @Schema(description = "Rôles à attribuer (remplace la liste existante). "
                + "Valeurs : ADMIN, MANAGER, USER.",
                example = "[\"USER\", \"MANAGER\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty List<String> roles
) {
}
