package com.enterprise.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête de connexion.
 */
public record LoginRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password
) {
}
