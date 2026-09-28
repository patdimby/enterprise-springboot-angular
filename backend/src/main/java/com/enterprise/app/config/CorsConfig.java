package com.enterprise.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuration CORS — l'autorisation pour le FRONTEND d'appeler l'API.
 *
 * <p><b>C'est quoi CORS ?</b> Une sécurité des navigateurs : par défaut, une
 * page servie depuis http://localhost:4200 (Angular) ne peut pas appeler une
 * API sur un AUTRE domaine/port (http://localhost:8080). Le navigateur bloque.
 * Ce fichier dit au navigateur : "ce frontend-là a le droit d'appeler cette
 * API". C'est le serveur qui déclare ses invités, via des en-têtes HTTP.</p>
 *
 * <p>Les origines autorisées viennent de app.cors-allowed-origins
 * (variable APP_CORS_ALLOWED_ORIGINS), définies par profil :
 * 4200 en dev (ng serve), 80 en Docker (nginx).</p>
 *
 * <p>@Configuration = "cette classe déclare des beans" (des objets mis à
 * disposition de toute l'application).</p>
 */
@Configuration
@RequiredArgsConstructor  // Lombok : constructeur généré (injection d'AppProperties)
public class CorsConfig {

    /**
     * La source de configuration CORS consultée par Spring Security
     * (SecurityConfig fait .cors(cors -> cors.configure(http))).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées : la liste "http://localhost:4200,http://localhost:80"
        // est découpée en tableau sur les virgules.
        String origins = appProperties.corsAllowedOrigins();
        if (origins != null && !origins.isBlank()) {
            config.setAllowedOrigins(Arrays.asList(origins.split(",")));
        } else {
            // Filet de sécurité si la propriété n'est pas définie du tout.
            config.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:8100"));
        }

        // Méthodes HTTP acceptées en cross-origin.
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Tous les en-têtes de requête acceptés (Authorization, Content-Type...).
        config.setAllowedHeaders(Arrays.asList("*"));
        // En-têtes de réponse que le JavaScript peut lire (le token JWT).
        config.setExposedHeaders(Arrays.asList("Authorization"));
        // Autorise les cookies/credentials cross-origin.
        config.setAllowCredentials(true);
        // Durée (en secondes) de mise en cache de la réponse CORS par le navigateur.
        config.setMaxAge(3600L);

        // Applique ces règles à TOUTES les routes (/**).
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
