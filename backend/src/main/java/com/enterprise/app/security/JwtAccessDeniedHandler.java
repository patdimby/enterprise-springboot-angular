package com.enterprise.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Réponse 403 JSON quand l'utilisateur EST authentifié mais n'a pas les
 * DROITS requis (ex. un USER simple qui appelle /api/users réservé aux ADMIN).
 *
 * <p>Différence avec {@link JwtAuthEntryPoint} :
 * 401 = "inconnu / non connecté" ; 403 = "connecté mais interdit".</p>
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {
                  "type": "about:blank",
                  "title": "Forbidden",
                  "status": 403,
                  "detail": "Droits insuffisants pour accéder à cette ressource.",
                  "instance": "%s",
                  "timestamp": "%s"
                }
                """.formatted(request.getRequestURI(), Instant.now()));
    }
}
