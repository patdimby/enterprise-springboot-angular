package com.enterprise.app.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps attendu par POST /api/auth/login.
 * JSON reçu : {@code {"email": "jane@example.com", "password": "Password123!"}}
 *
 * <p><b>Record</b> = classe immuable générée automatiquement (constructeur,
 * accesseurs, equals...). <b>@NotBlank/@Email/@Size</b> = règles de validation
 * vérifiées AVANT d'atteindre le contrôleur (@Valid) ; en cas d'échec,
 * GlobalExceptionHandler renvoie un 400 détaillé.</p>
 */
@Schema(description = "Identifiants de connexion (POST /api/auth/login). "
        + "En cas d'échec, la réponse 401 ne précise volontairement pas si "
        + "c'est l'email ou le mot de passe qui est faux (anti-énumération).")
public record LoginRequest(
        @Schema(description = "Email du compte (insensible à la casse).",
                example = "admin@enterprise.com", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255)
        @NotBlank @Email @Size(max = 255) String email,  // Non vide, format email, 255 max

        @Schema(description = "Mot de passe en clair — comparé au hash BCrypt en base.",
                example = "Admin123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password                         // Non vide
) {
}
