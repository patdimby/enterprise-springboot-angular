package com.enterprise.app.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Représentation publique d'un compte utilisateur — "
        + "renvoyée par register, /api/users/{id} et la liste paginée. "
        + "Ne contient JAMAIS le mot de passe (même hashé).")
public record UserResponse(
        @Schema(description = "Identifiant technique (généré par MySQL).",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Email de connexion (unique en base).",
                example = "jane.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Nom complet affiché dans l'interface.",
                example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,

        @Schema(description = "Compte actif ? Un compte désactivé (false) ne peut "
                + "plus se connecter jusqu'à réactivation.",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean enabled,       // Compte actif ou désactivé

        @Schema(description = "Rôles du compte parmi ADMIN, MANAGER, USER.",
                example = "[\"USER\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> roles,

        @Schema(description = "Date de création du compte (ISO 8601 UTC).",
                example = "2026-09-28T14:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt
) {
}
