package com.enterprise.app.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Réponse de POST /api/auth/login : le token JWT + l'identité de l'utilisateur
 * (pratique : le frontend n'a pas besoin d'un second appel pour afficher le nom).
 *
 * <p>JSON envoyé :
 * {@code {"token":"eyJhbG...","type":"Bearer","id":1,"email":"...",
 * "fullName":"Jane Doe","roles":["USER"]}}</p>
 *
 * <p>Le client rangera le token et le renverra à chaque requête dans
 * l'en-tête {@code Authorization: Bearer <token>}.</p>
 */
@Schema(description = "Session émise après une connexion réussie : le token JWT "
        + "à porter dans l'en-tête 'Authorization: Bearer <token>', plus "
        + "l'identité de l'utilisateur.")
public record LoginResponse(
        @Schema(description = "JWT signé HS256, valable 24 h (configurable via "
                + "app.jwt-expiration). Contient les claims id, email, fullName, roles.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbnRlcnByaXNlLmNvbSIsImlkIjoxfQ.abc123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String token,        // Le JWT signé

        @Schema(description = "Type d'authentification HTTP — toujours \"Bearer\".",
                example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
        String type,         // Toujours "Bearer" (type d'authentification HTTP)

        @Schema(description = "Identifiant technique du compte.",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Email du compte.",
                example = "admin@enterprise.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Nom complet affiché dans l'interface.",
                example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,

        @Schema(description = "Rôles du compte (copiés dans le JWT) : ADMIN, "
                + "MANAGER et/ou USER.",
                example = "[\"ADMIN\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> roles   // Ex. ["USER"] ou ["ADMIN"]
) {
}
