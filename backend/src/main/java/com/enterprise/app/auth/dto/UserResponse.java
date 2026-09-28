package com.enterprise.app.auth.dto;

import java.time.Instant;
import java.util.List;

/**
 * Représentation PUBLIQUE d'un utilisateur — le DTO renvoyé par les endpoints
 * register, /api/users... Il ne contient JAMAIS le hash du mot de passe
 * (règle d'or : les entités JPA ne sortent jamais de l'API, uniquement des DTO).
 *
 * <p>Instant = un instant précis sur l'horloge mondiale ; sérialisé en ISO :
 * {@code "createdAt":"2026-09-28T14:30:00Z"} (le Z = UTC).</p>
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        boolean enabled,       // Compte actif ou désactivé
        List<String> roles,
        Instant createdAt
) {
}
