package com.enterprise.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps attendu par POST /api/auth/register.
 * JSON reçu : {@code {"email":"jane@example.com","password":"Password123!",
 * "fullName":"Jane Doe"}}
 *
 * <p>Les règles de validation (non vide, format email, longueur du mot de
 * passe 8 à 100...) sont déclenchées par {@code @Valid} dans le contrôleur.</p>
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String fullName
) {
}
