package com.enterprise.app.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * FILTRE D'AUTHENTIFICATION JWT — le portier automatique de chaque requête.
 *
 * <p><b>C'est quoi un filtre ?</b> Un bout de code exécuté pour CHAQUE requête
 * HTTP, AVANT le contrôleur. {@code OncePerRequestFilter} garantit "une seule
 * fois par requête". Ce filtre :</p>
 * <ol>
 *   <li>regarde s'il y a un en-tête {@code Authorization: Bearer <token>} ;</li>
 *   <li>si oui, vérifie le token via {@link JwtService} ;</li>
 *   <li>si le token est valide, installe l'utilisateur dans le
 *       SecurityContext (la "mémoire" de Spring Security pour la requête) ;
 *       le contrôleur peut alors savoir QUI appelle (et le rôle).</li>
 * </ol>
 *
 * <p>API stateless : rien n'est mis en session serveur. Chaque requête se
 * justifie elle-même avec son token.</p>
 */
@Component            // Spring crée le filtre ; SecurityConfig l'ajoute à la chaîne
@Slf4j                // Lombok : champ "log" généré
@RequiredArgsConstructor  // Lombok : constructeur avec jwtService (injection)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_ID = "id";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;

    /**
     * Le cœur du filtre, exécuté une fois par requête.
     * {@code @NonNull} = contrat "jamais null" (aide les outils, pas un contrôle).
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Y a-t-il un en-tête Authorization commençant par "Bearer " ?
        //    Non (ex. Swagger ouvert sans login) → on passe au filtre suivant
        //    sans rien faire ; la sécurité décidera plus tard (401 si protégé).
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. On isole le token (on retire le préfixe "Bearer ").
        String token = header.substring(BEARER_PREFIX.length());
        Claims claims = jwtService.parseToken(token);

        // 3. Token valide ET requête pas déjà authentifiée → on installe l'utilisateur.
        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Convertit les rôles du token ["USER"] en authorities "ROLE_USER"
            // (le format attendu par hasRole('USER') dans la config de sécurité).
            List<SimpleGrantedAuthority> authorities = extractRoles(claims).stream()
                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                    .toList();

            // Reconstruit l'identité de l'utilisateur depuis les claims du token
            // (pas besoin de reconsulter la base : c'est l'intérêt du JWT).
            UserPrincipal principal = UserPrincipal.builder()
                    .id(((Number) claims.get(CLAIM_ID)).longValue())
                    .email(claims.getSubject())
                    .fullName((String) claims.get(CLAIM_FULL_NAME))
                    .build();

            // "Principale" = l'utilisateur + "null" = credentials (déjà vérifiées)
            // + authorities = ses rôles.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // L'utilisateur est maintenant "connecté" pour cette requête.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (claims == null) {
            // Token présent mais invalide (falsifié, expiré...) → simple log.
            log.debug("Requête avec token JWT invalide sur {}", request.getRequestURI());
        }

        // 4. On laisse continuer la requête (vers le contrôleur ou le prochain filtre).
        filterChain.doFilter(request, response);
    }

    /** Lit le claim "roles" en gérant le cas où il est absent ou mal typé. */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
