package com.enterprise.app.security;

import com.enterprise.app.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * GÉNÉRATION ET VALIDATION DES TOKENS JWT.
 *
 * <p><b>C'est quoi un JWT ?</b> Un "badge d'accès" signé que le serveur remet
 * à l'utilisateur après le login. À chaque requête, le client le renvoie dans
 * l'en-tête {@code Authorization: Bearer <token>}. Le serveur vérifie la
 * SIGNATURE (avec notre secret) : si elle est valide, le token n'a pas été
 * falsifié. Le token contient aussi des "claims" (id, email, rôles...) que
 * le serveur lit sans reconsulter la base.</p>
 *
 * <p>Format : 3 blocs base64 séparés par des points —
 * {@code en-tête.payload.signature}.</p>
 *
 * <p>Bibliothèque utilisée : JJWT ({@code io.jsonwebtoken}).</p>
 */
@Service
@Slf4j                    // Lombok : génère le champ "log" (log.info, log.debug...)
@RequiredArgsConstructor  // Lombok : constructeur avec tous les champs final
public class JwtService {

    /** Nom des "claims" (les infos rangées dans le token). */
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_FULL_NAME = "fullName";

    private final AppProperties appProperties;

    /**
     * Construit la clé de signature à partir du secret (config "app.jwt-secret").
     * HS256 = algorithme de signature symétrique : la MÊME clé sert à signer
     * (serveur) et à vérifier. D'où l'importance de garder le secret... secret !
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(appProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** Durée de vie des tokens en millisecondes (config "app.jwt-expiration"). */
    private long expirationMs() {
        return appProperties.jwtExpiration().toMillis();
    }

    /**
     * Fabrique un token signé pour un utilisateur après son login.
     *
     * @param userId   stocké dans le claim "id"
     * @param email    stocké dans le "subject" (le sujet du token)
     * @param fullName stocké dans le claim "fullName"
     * @param roles    stocké dans le claim "roles" (ex. ["USER"])
     * @return le token JWT complet, prêt à être renvoyé au client
     */
    public String generateToken(Long userId, String email, String fullName, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)                                   // À qui appartient le token
                .claim("id", userId)                              // Infos personnalisées...
                .claim(CLAIM_FULL_NAME, fullName)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))                         // Émis quand ? Maintenant
                .expiration(Date.from(now.plusMillis(expirationMs()))) // Expire quand ? +24h
                .signWith(key())                                  // Signature avec NOTRE clé
                .compact();                                       // Fabrique la chaîne finale
    }

    /**
     * Vérifie la signature et l'expiration d'un token, puis renvoie son contenu.
     *
     * @return les claims si le token est valide, {@code null} sinon
     * (le filtre d'authentification ignore alors la requête)
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())          // Vérifie la signature avec la même clé
                    .build()
                    .parseSignedClaims(token)   // Décode + vérifie l'expiration
                    .getPayload();              // Récupère le contenu (payload)
        } catch (JwtException | IllegalArgumentException e) {
            // Token falsifié, expiré ou mal formé : on loggue en DEBUG
            // (pas la peine de remplir les logs en WARN avec les bots du web)
            // et on renvoie null — l'appelant traitera la requête comme non authentifiée.
            log.debug("Token JWT invalide : {}", e.getMessage());
            return null;
        }
    }
}
