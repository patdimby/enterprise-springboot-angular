package com.enterprise.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Réponse 401 JSON (format RFC 7807) quand un accès protégé est demandé
 * SANS authentification valide — au lieu de la page d'erreur blanche par défaut.
 *
 * <p><b>Quand est-il appelé ?</b> Spring Security vérifie les règles de
 * SecurityConfig : si la route exige d'être connecté et que le filtre JWT
 * n'a rien installé dans le SecurityContext, ce composant écrit la réponse.</p>
 *
 * <p>Exemple de réponse : {@code {"title":"Unauthorized","status":401,
 * "detail":"Authentification requise : fournissez un en-tête Authorization: Bearer <token>."}}</p>
 */
@Component  // Un seul exemplaire, injecté dans SecurityConfig
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Code HTTP 401 = "je ne sais pas qui tu es, authentifie-toi".
        // (403 = "je sais qui tu es, mais tu n'as pas le droit" → JwtAccessDeniedHandler)
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {
                  "type": "about:blank",
                  "title": "Unauthorized",
                  "status": 401,
                  "detail": "Authentification requise : fournissez un en-tête Authorization: Bearer <token>.",
                  "instance": "%s",
                  "timestamp": "%s"
                }
                """.formatted(request.getRequestURI(), Instant.now()));
    }
}
