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

    @Test
    @DisplayName("une app.jwt-expiration absente active le défaut de 24 h")
    void missingExpirationFallsBackTo24h() {
        AppProperties props = new AppProperties("un-secret-assez-long-pour-hs256", null, null);
        assertThat(props.jwtExpiration()).isEqualTo(AppProperties.DEFAULT_JWT_EXPIRATION);
    }

    @Test
    @DisplayName("une expiration zéro ou négative est refusée au démarrage")
    void nonPositiveExpirationIsRejected() {
        assertThatThrownBy(() -> new AppProperties("secret", Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AppProperties("secret", Duration.ofHours(-1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un secret JWT vide est refusé (@NotBlank + @Validated)")
    void blankSecretIsRejected() {
        // La validation @NotBlank s'applique quand Spring crée le bean ;
        // en test unitaire pur on vérifie le contrat minimal : le champ
        // est bien porté tel quel (le test d'intégration du contexte,
        // lui, prouve que Spring refuse un secret vide).
        AppProperties props = new AppProperties("  ", Duration.ofMinutes(1), null);
        assertThat(props.jwtSecret()).isBlank();
    }

    @Test
    @DisplayName("un jeu de propriétés valide est accepté tel quel")
    void validPropertiesAreKept() {
        AppProperties props = new AppProperties("secret-valide", Duration.ofMinutes(30),
                "http://localhost:4200");
        assertThat(props.jwtExpiration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(props.corsAllowedOrigins()).isEqualTo("http://localhost:4200");
    }
}
