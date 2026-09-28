package com.enterprise.app.security;

import com.enterprise.app.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Génération et validation des tokens JWT (HS256, bibliothèque JJWT).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_FULL_NAME = "fullName";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties appProperties) {
        this.key = Keys.hmacShaKeyFor(appProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = appProperties.jwtExpiration().toMillis();
    }

    /**
     * Construit un token signé pour l'utilisateur : subject = email, claims = id, nom, rôles.
     */
    public String generateToken(Long userId, String email, String fullName, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("id", userId)
                .claim(CLAIM_FULL_NAME, fullName)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Valide la signature et l'expiration ; renvoie les claims ou {@code null} si invalide.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT invalide : {}", e.getMessage());
            return null;
        }
    }
}
