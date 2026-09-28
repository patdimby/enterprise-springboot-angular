package com.enterprise.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Propriétés applicatives custom — le pont entre le fichier de configuration
 * et le code Java.
 *
 * <p><b>C'est quoi @ConfigurationProperties ?</b> Ça dit à Spring : "prends
 * tout ce qui est sous le préfixe {@code app} dans application*.yml et
 * remplis les champs de ce record". Exemple :
 * {@code app.jwt-expiration: 24h} → {@code jwtExpiration = Duration de 24h}.</p>
 *
 * <p>C'est un record (immuable) : une fois rempli au démarrage, plus personne
 * ne peut le modifier — pratique pour une configuration.</p>
 *
 * <p>Les valeurs peuvent venir du yml OU de variables d'environnement
 * (APP_JWT_SECRET, APP_JWT_EXPIRATION...), selon le profil actif
 * (dev, test, prod, docker).</p>
 *
 * <p>Le record est scanné automatiquement grâce à @ConfigurationPropertiesScan
 * sur EnterpriseApplication.</p>
 */
@Slf4j                    // Lombok : champ "log" généré (utilisé plus bas)
@Validated                // Active la validation des champs (@NotBlank ci-dessous)
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        /** Secret de signature des JWT — @NotBlank fait échouer le démarrage s'il manque. */
        @NotBlank String jwtSecret,
        /** Durée de vie des tokens (ex. "24h", "30m"). */
        Duration jwtExpiration,
        /** Origines CORS autorisées, séparées par des virgules. */
        String corsAllowedOrigins
) {
    /** Si app.jwt-expiration n'est pas défini : 24 h par défaut. */
    public static final Duration DEFAULT_JWT_EXPIRATION = Duration.ofHours(24);

    /**
     * "Constructeur compact" du record : exécuté à la création, il valide et
     * complète les valeurs. C'est ici qu'on refuse une configuration absurde
     * (ex. expiration négative) — tôt, avant que ça cause des bugs.
     */
    public AppProperties {
        if (jwtExpiration == null) {
            log.info("app.jwt-expiration non défini : valeur par défaut 24h appliquée");
            jwtExpiration = DEFAULT_JWT_EXPIRATION;
        }
        if (jwtExpiration.isZero() || jwtExpiration.isNegative()) {
            throw new IllegalArgumentException("app.jwt-expiration doit être strictement positif");
        }
        // Bonne pratique cryptographique : HS256 exige une clé d'AU MOINS
        // 256 bits (32 octets). Un secret trop court est refusé dès le
        // démarrage plutôt que d'exposer des tokens falsifiables.
        // (Le test profile d'AppPropertiesTest vérifie ce contrat.)
        if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt-secret doit contenir au moins 32 octets (256 bits) : "
                            + "générez-en un avec `openssl rand -base64 64`");
        }
    }
}
