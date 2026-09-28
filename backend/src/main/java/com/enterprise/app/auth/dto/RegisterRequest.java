package com.enterprise.app.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps attendu par POST /api/auth/register.
 * JSON reçu : {@code {"email":"jane@example.com","password":"Password123!",
 * "fullName":"Jane Doe"}}
 *
 * <p>Les règles de validation (non vide, format email, longueur du mot de
 * passe 8 à 100...) sont déclenchées par {@code @Valid} dans le contrôleur ;
 * les annotations {@code @Schema} documentent chaque champ dans Swagger
 * (description, exemple, valeur par défaut...).</p>
 */
@Schema(description = "Identité du nouveau compte à créer (POST /api/auth/register). "
        + "Le rôle USER est attribué automatiquement.")
public record RegisterRequest(
        @Schema(description = "Email de connexion, unique en base. Insensible à la casse "
                + "pour la connexion (Jean@Mail.com = jean@mail.com).",
                example = "jane.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255)
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(description = "Mot de passe en clair (8 à 100 caractères). Il est "
                + "immédiatement hashé en BCrypt et jamais stocké ni renvoyé tel quel.",
                example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 100)
        @NotBlank @Size(min = 8, max = 100) String password,

        @Schema(description = "Nom complet affiché dans l'interface.",
                example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank @Size(max = 100) String fullName
) {
}
