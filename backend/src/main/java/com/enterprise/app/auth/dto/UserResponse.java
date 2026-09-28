package com.enterprise.app.auth.dto;

import java.time.Instant;
import java.util.List;

/**
 * Représentation publique d'un utilisateur (sans le hash du mot de passe).
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        boolean enabled,
        List<String> roles,
        Instant createdAt
) {
}
