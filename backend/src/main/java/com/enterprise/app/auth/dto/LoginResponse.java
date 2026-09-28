package com.enterprise.app.auth.dto;

import java.util.List;

/**
 * Réponse de connexion : token JWT + identité de l'utilisateur.
 */
public record LoginResponse(
        String token,
        String type,
        Long id,
        String email,
        String fullName,
        List<String> roles
) {
}
