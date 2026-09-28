package com.enterprise.app.user;

import jakarta.validation.constraints.NotNull;

/**
 * Requête d'activation/désactivation d'un compte utilisateur.
 */
public record UpdateEnabledRequest(
        @NotNull Boolean enabled
) {
}
