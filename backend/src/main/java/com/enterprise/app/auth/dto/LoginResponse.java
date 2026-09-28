package com.enterprise.app.auth.dto;

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
public record LoginResponse(
        String token,        // Le JWT signé
        String type,         // Toujours "Bearer" (type d'authentification HTTP)
        Long id,
        String email,
        String fullName,
        List<String> roles   // Ex. ["USER"] ou ["ADMIN"]
) {
}
