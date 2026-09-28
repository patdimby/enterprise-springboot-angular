package com.enterprise.app.auth;

import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import com.enterprise.app.security.JwtService;
import com.enterprise.app.user.Role;
import com.enterprise.app.user.RoleName;
import com.enterprise.app.user.RoleRepository;
import com.enterprise.app.user.User;
import com.enterprise.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Logique métier de l'AUTHENTIFICATION : inscription et connexion.
 *
 * <p><b>Inscription</b> : vérifier l'email libre → hasher le mot de passe
 * (BCrypt) → enregistrer avec le rôle USER.</p>
 *
 * <p><b>Connexion</b> : confier email + mot de passe à l'AuthenticationManager
 * de Spring Security (qui passe par AppUserDetailsService pour charger le
 * compte et BCrypt pour comparer les hash) → si OK, fabriquer un JWT via
 * {@link JwtService}.</p>
 */
@Service
@Slf4j                    // Lombok : champ "log" généré
@RequiredArgsConstructor  // Lombok : constructeur avec tous les champs final (injection)
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Crée un compte (rôle USER par défaut).
     *
     * @throws ResponseStatusException 409 si l'email est déjà pris
     */
    @Transactional  // TOUT réussit ou TOUT est annulé (pas de compte à moitié créé)
    public UserResponse register(RegisterRequest request) {
        // 1. Email déjà utilisé ? → 409 CONFLICT.
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé : " + request.email());
        }

        // 2. Le rôle USER est créé au démarrage (DataInitializer).
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Rôle USER manquant en base"));

        // 3. On ne stocke JAMAIS le mot de passe en clair :
        //    encode() produit un hash BCrypt irréversible.
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        User saved = userRepository.save(user);
        log.info("Nouvel utilisateur inscrit : {}", saved.getEmail());

        // 4. Réponse = DTO sans le mot de passe.
        return toResponse(saved);
    }

    /**
     * Vérifie les identifiants et fabrique le token JWT.
     *
     * @throws BadCredentialsException 401 si email ou mot de passe incorrect
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            // On confie la vérification à Spring Security :
            // AuthenticationManager → AppUserDetailsService (charge le compte)
            // → PasswordEncoder (compare les hash BCrypt).
            // Le UsernamePasswordAuthenticationToken ici sert juste de
            // "transport" pour email + mot de passe.
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.email(), request.password()));
        } catch (AuthenticationException e) {
            // Message VOLONTAirement vague : ne pas révéler si l'email existe
            // (sinon un attaquant peut deviner les comptes valides).
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        // Authentification réussie : on recharge l'entité complète
        // (le principal retourné est un UserDetails Spring, pas notre entité).
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));

        // 5. Construit le JWT : email + id + nom + rôles, signé, valable 24 h.
        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getFullName(), roles);
        return new LoginResponse(token, "Bearer", user.getId(), user.getEmail(), user.getFullName(), roles);
    }

    /** Entité → DTO (jamais de mot de passe dans la réponse). */
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isEnabled(),
                user.getRoles().stream().map(r -> r.getName().name()).toList(),
                user.getCreatedAt());
    }
}
