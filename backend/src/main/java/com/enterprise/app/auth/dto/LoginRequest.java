package com.enterprise.app.auth.dto;

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
public record LoginRequest(
        @NotBlank @Email @Size(max = 255) String email,  // Non vide, format email, 255 max
        @NotBlank String password                         // Non vide
) {
}
