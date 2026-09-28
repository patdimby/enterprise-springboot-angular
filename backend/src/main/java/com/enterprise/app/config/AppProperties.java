package com.enterprise.app.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Propriétés applicatives (préfixe {@code app}).
 *
 * <p>Injectées depuis {@code application.yml} ou les variables d'environnement :
 * {@code APP_JWT_SECRET}, {@code APP_JWT_EXPIRATION}, {@code APP_CORS_ALLOWED_ORIGINS}.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String jwtSecret,
        Duration jwtExpiration,
        String corsAllowedOrigins
) {
    /** Durée de vie par défaut des tokens : 24 h. */
    public static final Duration DEFAULT_JWT_EXPIRATION = Duration.ofHours(24);

    public AppProperties {
        if (jwtExpiration == null) {
            jwtExpiration = DEFAULT_JWT_EXPIRATION;
        }
        if (jwtExpiration.isZero() || jwtExpiration.isNegative()) {
            throw new IllegalArgumentException("app.jwt-expiration doit être strictement positif");
        }
    }
}
