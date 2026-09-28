package com.enterprise.app.auth;

import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import com.enterprise.app.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publics d'AUTHENTIFICATION + profil de l'utilisateur courant.
 *
 * <p>Trois routes :</p>
 * <ul>
 *   <li>POST /api/auth/register — créer un compte (rôle USER) ;</li>
 *   <li>POST /api/auth/login — se connecter et recevoir un token JWT ;</li>
 *   <li>GET /api/auth/me — "qui suis-je ?" (token requis).</li>
 * </ul>
 *
 * <p>Ces routes sont publiques (register/login) ou protégées par JWT (me) —
 * voir les règles dans SecurityConfig.</p>
 */
@RestController
@RequestMapping("/api/auth")  // Préfixe commun : /api/auth/...
@Tag(name = "Authentification", description = "Inscription, connexion et profil courant")  // Swagger
@RequiredArgsConstructor  // Lombok : constructeur généré (injection de authService)
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * {@code @Valid} : vérifie les règles des annotations (@Email, @Size...)
     * déclarées dans RegisterRequest. JSON invalide → 400 automatique.
     * {@code @RequestBody} : convertit le JSON reçu en objet Java.
     */
    @PostMapping("/register")
    @Operation(summary = "Créer un compte (rôle USER)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        // 201 CREATED : convention REST pour "ressource créée".
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login — renvoie le token JWT + l'identité.
     * Identifiants incorrects → 401 (BadCredentialsException, gérée par
     * GlobalExceptionHandler).
     */
    @PostMapping("/login")
    @Operation(summary = "Se connecter et récupérer un token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * GET /api/auth/me — profil de l'utilisateur DU token envoyé.
     *
     * <p>{@code @AuthenticationPrincipal} : Spring injecte directement
     * l'identité reconstruite par le filtre JWT (UserPrincipal).
     * Pas de token → la requête n'arrive jamais ici : 401.</p>
     */
    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur courant (JWT requis)")
    public ResponseEntity<UserPrincipal> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(principal);
    }
}
