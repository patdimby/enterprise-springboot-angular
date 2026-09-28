package com.enterprise.app.security;

import com.enterprise.app.config.AppProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTS UNITAIRES de {@link JwtService}.
 *
 * <p><b>Test unitaire vs test d'intégration :</b> ici on teste UNE classe
 * isolée, sans démarrer Spring ni la base — c'est rapide (millisecondes).
 * On fabrique à la main les objets dont la classe a besoin (ici une
 * {@link AppProperties} avec un secret de test).</p>
 *
 * <p>On vérifie le contrat complet : contenu des claims, expiration,
 * rejet des tokens falsifiés / expirés / absurdes.</p>
 */
@DisplayName("JwtService — génération et validation des JWT")
class JwtServiceTest {

    /** Secret de test : 256+ bits (32 octets minimum, exigé par AppProperties). */
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMjU2Yml0cyEh";

    /** Instance testée — construite à la main, sans Spring. */
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Record immuable : on en crée un avec une durée COURTE pour tester
        // l'expiration sans attendre 24 heures.
        AppProperties properties = new AppProperties(TEST_SECRET, Duration.ofMinutes(5), "http://localhost:4200");
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("generateToken produit un JWT dont les claims correspondent à l'utilisateur")
    void generateTokenContainsUserClaims() {
        String token = jwtService.generateToken(42L, "jane@example.com", "Jane Doe", List.of("USER", "MANAGER"));

        // Un JWT ressemble à "en-tête.payload.signature" : 3 blocs séparés par des points.
        assertThat(token).matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

        // Le token se re-lit avec le même service (même clé de signature).
        Claims claims = jwtService.parseToken(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("jane@example.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(42L);
        assertThat(claims.get("fullName", String.class)).isEqualTo("Jane Doe");
        assertThat(claims.get("roles", List.class)).containsExactly("USER", "MANAGER");
    }

    @Test
    @DisplayName("le token expire bien au bout de la durée configurée (± marge d'horloge)")
    void tokenExpiresAfterConfiguredDuration() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(1L, "a@b.co", "A", List.of("USER"));
        long after = System.currentTimeMillis();

        Claims claims = jwtService.parseToken(token);
        // Expiration attendue : entre "before + 5 min" et "after + 5 min",
        // avec 1 s de tolérance : JJWT tronque la date en secondes, ce qui
        // peut retirer jusqu'à 999 ms à l'expiration stockée.
        long expectedMin = before + Duration.ofMinutes(5).toMillis() - 1_000;
        long expectedMax = after + Duration.ofMinutes(5).toMillis();
        assertThat(claims.getExpiration().getTime())
                .isBetween(expectedMin, expectedMax);
    }

    @Test
    @DisplayName("parseToken rejette un token signé avec une autre clé")
    void parseTokenRejectsForeignSignature() {
        // Le même algorithme, mais signé par un "attaquant" avec un autre secret
        // (256+ bits pour passer la validation d'AppProperties).
        AppProperties impostorProps =
                new AppProperties("c2VjcmV0LWZhbHNlLXNlY3JldC1mYWxzZS1zZWNyZXQtZmFsc2UtMjU2Yml0cw==",
                        Duration.ofMinutes(5), null);
        JwtService impostor = new JwtService(impostorProps);

        String forgedToken = impostor.generateToken(1L, "hacker@example.com", "Hacker", List.of("ADMIN"));

        // Notre service refuse ce token : la signature ne correspond pas à NOTRE clé.
        assertThat(jwtService.parseToken(forgedToken)).isNull();
    }

    @Test
    @DisplayName("parseToken renvoie null pour une chaîne qui n'est pas un JWT")
    void parseTokenRejectsGarbage() {
        assertThat(jwtService.parseToken("not-a-jwt")).isNull();
        assertThat(jwtService.parseToken("")).isNull();
        assertThat(jwtService.parseToken("aaa.bbb.ccc")).isNull();
    }

    @Test
    @DisplayName("un token déjà expiré à l'émission est refusé")
    void expiredTokenIsRejected() {
        // Astuce de test : on signe un token "pour dans le passé" en créant le
        // service avec une expiration d'1 ms puis en laissant l'horloge avancer.
        // Le constructeur d'AppProperties refuse 0 et le négatif, mais 1 ms
        // passe — et le token est expiré avant même qu'on le lise.
        AppProperties expiredProps = new AppProperties(TEST_SECRET, Duration.ofNanos(1), null);
        JwtService expiredIssuer = new JwtService(expiredProps);

        String token = expiredIssuer.generateToken(1L, "old@example.com", "Old", List.of("USER"));

        // Token périmé : la lecture échoue (ExpiredJwtException → null).
        assertThat(jwtService.parseToken(token)).isNull();
    }
}
