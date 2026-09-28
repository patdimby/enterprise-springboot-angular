package com.enterprise.app.auth;

import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import com.enterprise.app.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * voir les règles dans SecurityConfig. La DOCUMENTATION Swagger de chaque
 * endpoint (résumé, réponses possibles, sécurité) est déclarée juste
 * au-dessus de chaque méthode avec les annotations io.swagger.v3.oas.*.</p>
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentification",
        description = "Inscription, connexion et profil courant. "
                + "Register et login sont publics ; /me exige le JWT (bouton Authorize).")
@RequiredArgsConstructor  // Lombok : constructeur généré (injection de authService)
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register — création d'un compte.
     * {@code @Valid} : vérifie les règles des annotations (@Email, @Size...)
     * déclarées dans RegisterRequest. JSON invalide → 400.
     * {@code @RequestBody} : convertit le JSON reçu en objet Java.
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Créer un compte (rôle USER)",
            description = """
                    Inscrit un nouvel utilisateur.

                    **Étapes internes :**
                    1. Vérifie que l'email n'est pas déjà pris (sinon **409**) ;
                    2. Hash le mot de passe avec **BCrypt** (jamais stocké en clair) ;
                    3. Attribue le rôle **USER** par défaut ;
                    4. Persiste le compte et renvoie le DTO public (sans mot de passe).

                    Le mot de passe doit contenir entre 8 et 100 caractères.
                    Pour s'authentifier ensuite, enchaîner avec `POST /api/auth/login`.
                    """,
            security = {}) // Endpoint PUBLIC : pas de bouton "Authorize" requis ici.
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé — corps : UserResponse",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide (validation : email mal formé, mot de passe < 8 caractères, champ manquant) — corps : ProblemDetail avec carte errors"),
            @ApiResponse(responseCode = "409", description = "Email déjà utilisé — corps : ProblemDetail (detail = 'Email déjà utilisé : …')")
    })
    public ResponseEntity<UserResponse> register(
            @Parameter(description = "Identité du nouveau compte (email, mot de passe, nom complet)", required = true)
            @Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        // 201 CREATED : convention REST pour "ressource créée".
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login — renvoie le token JWT + l'identité.
     * Identifiants incorrects → 401 (BadCredentialsException, gérée par
     * GlobalExceptionHandler). Message volontairement vague : on ne révèle
     * pas si l'email existe (anti-énumération de comptes).
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Se connecter et récupérer un token JWT",
            description = """
                    Vérifie les identifiants puis émet un **JWT signé HS256** valable 24 h.

                    **Réponse :** `LoginResponse` — le `token` à porter ensuite dans
                    l'en-tête `Authorization: Bearer <token>`, plus l'identité
                    (id, email, fullName, roles) pour éviter un appel à `/me`.

                    **Sécurité :** en cas d'échec, le message est volontairement vague
                    ("Email ou mot de passe incorrect") pour ne pas révéler si le
                    compte existe.

                    **Flux interne :** AuthenticationManager (Spring Security) →
                    AppUserDetailsService (charge le compte) → BCrypt (compare les
                    hash) → JwtService (signe le token).
                    """,
            security = {}) // Endpoint PUBLIC.
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie — corps : LoginResponse (token + profil)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Corps illisible ou champ manquant — corps : ProblemDetail"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect — corps : ProblemDetail (message vague volontaire)")
    })
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "Identifiants de connexion", required = true)
            @Valid @RequestBody LoginRequest request) {
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
    @Operation(
            summary = "Profil de l'utilisateur courant (JWT requis)",
            description = """
                    Renvoie l'identité **reconstruite depuis le token JWT** envoyé
                    (claims `id`, `email`, `fullName`) — aucune lecture base :

                    1. Le filtre `JwtAuthenticationFilter` valide la signature et
                       l'expiration du token de l'en-tête `Authorization: Bearer ...` ;
                    2. Si valide, le principal est posé dans le SecurityContext ;
                    3. Cette méthode le renvoie tel quel.

                    Idéal pour vérifier "mon token est-il encore valide ?" au
                    démarrage du frontend.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Identité de l'appelant — corps : UserPrincipal (id, email, fullName)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserPrincipal.class))),
            @ApiResponse(responseCode = "401", description = "Token absent, expiré ou invalide — corps : ProblemDetail JSON (JwtAuthEntryPoint)")
    })
    public ResponseEntity<UserPrincipal> me(
            // "hidden" : ce paramètre est résolu par Spring, pas par le client —
            // inutile de l'afficher dans Swagger.
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(principal);
    }
}
