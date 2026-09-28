package com.enterprise.app.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TESTS UNITAIRES du record de configuration {@link AppProperties} :
 * valeurs par défaut, validations refusées (durée absurde, secret vide).
 */
@DisplayName("AppProperties — validation de la configuration")
class AppPropertiesTest {

    /** Secret de test : 256+ bits, comme l'exige la validation du record. */
    private static final String TEST_SECRET = "secret-de-test-assez-long-pour-hs256-32-octets!";

    @Test
    @DisplayName("une app.jwt-expiration absente active le défaut de 24 h")
    void missingExpirationFallsBackTo24h() {
        AppProperties props = new AppProperties(TEST_SECRET, null, null);
        assertThat(props.jwtExpiration()).isEqualTo(AppProperties.DEFAULT_JWT_EXPIRATION);
    }

    @Test
    @DisplayName("une expiration zéro ou négative est refusée au démarrage")
    void nonPositiveExpirationIsRejected() {
        assertThatThrownBy(() -> new AppProperties(TEST_SECRET, Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AppProperties(TEST_SECRET, Duration.ofHours(-1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un secret JWT vide est refusé (@NotBlank + @Validated)")
    void blankSecretIsRejected() {
        // Le constructeur compact refuse explicitement tout secret de moins
        // de 32 octets — un secret vide tombe dans ce cas.
        assertThatThrownBy(() -> new AppProperties("  ", Duration.ofMinutes(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un secret JWT trop court (< 256 bits) est refusé au démarrage")
    void shortSecretIsRejected() {
        // HS256 exige au moins 32 octets : un secret court serait falsifiable.
        assertThatThrownBy(() -> new AppProperties("trop-court", Duration.ofMinutes(1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 octets");
    }

    @Test
    @DisplayName("un jeu de propriétés valide est accepté tel quel")
    void validPropertiesAreKept() {
        // "secret-valide-assez-long-pour-hs256-32octets" = 43 caractères > 32 octets.
        AppProperties props = new AppProperties("secret-valide-assez-long-pour-hs256-32octets",
                Duration.ofMinutes(30), "http://localhost:4200");
        assertThat(props.jwtExpiration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(props.corsAllowedOrigins()).isEqualTo("http://localhost:4200");
    }
}
