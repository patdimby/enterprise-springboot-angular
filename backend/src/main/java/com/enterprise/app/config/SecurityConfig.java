package com.enterprise.app.config;

import com.enterprise.app.security.JwtAccessDeniedHandler;
import com.enterprise.app.security.JwtAuthEntryPoint;
import com.enterprise.app.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * LE CERVEAU DE LA SÉCURITÉ — toute la politique d'accès de l'API.
 *
 * <p>Les 3 annotations sur la classe :</p>
 * <ul>
 *   <li>{@code @Configuration} : déclare des beans (les méthodes @Bean) ;</li>
 *   <li>{@code @EnableWebSecurity} : active Spring Security et sa chaîne de filtres ;</li>
 *   <li>{@code @EnableMethodSecurity} : active {@code @PreAuthorize} sur les
 *       contrôleurs (ex. hasRole('ADMIN') dans UserController).</li>
 * </ul>
 *
 * <p>API 100% STATELESS : pas de session, pas de cookie — chaque requête
 * prouve qui elle est avec son JWT (installé par le filtre).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor  // Lombok : constructeur généré (injection des 3 composants)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * La chaîne de filtres = la liste des règles, lues DE HAUT EN BAS
     * (la première règle qui correspond s'applique).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF = protection anti-falsification pour les applications à
                // session/cookie. Inutile ici : API stateless avec token en en-tête.
                .csrf(csrf -> csrf.disable())
                // Branche notre configuration CORS (CorsConfig).
                .cors(cors -> cors.configure(http))
                // STATELESS = ne jamais créer de session HTTP.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Qui répond en cas d'échec :
                // pas connecté → JwtAuthEntryPoint (401), droits insuffisants → JwtAccessDeniedHandler (403).
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                // Les règles d'accès, de la plus spécifique à la plus générale :
                .authorizeHttpRequests(auth -> auth
                        // Documentation API : publique (Swagger).
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Supervision : le healthcheck Docker doit pouvoir répondre.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Inscription et connexion : forcément publiques !
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // Administration des comptes : ADMIN uniquement.
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // Tout le reste : il faut UN token valide (peu importe le rôle).
                        .anyRequest().authenticated())
                // Insère notre filtre JWT AVANT le filtre standard de login
                // (le login par formulaire ne sert pas ici, mais son filtre
                // marque l'ordre de la chaîne).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * L'encodeur de mots de passe : BCrypt. C'est le bean utilisé partout où
     * on écrit .encode(...) (inscription, seed admin) et par Spring Security
     * au login pour comparer les hash.
     * BCrypt intègre un "sel" aléatoire : deux fois le même mot de passe
     * produit deux hash différents — et c'est normal !
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Le "portier" utilisé par AuthService.login() : on lui tend email +
     * mot de passe, il charge le compte (via AppUserDetailsService), vérifie
     * le hash, l'état du compte... et accepte ou lève BadCredentialsException.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
