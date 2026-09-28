package com.enterprise.app.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * GESTIONNAIRE GLOBAL D'ERREURS — traduit TOUTES les exceptions Java en
 * réponses JSON propres (format RFC 7807 "Problem Detail").
 *
 * <p><b>C'est quoi un @RestControllerAdvice ?</b> Un filet de sécurité
 * appliqué à TOUS les contrôleurs : quand une exception est levée n'importe où
 * dans le code des contrôleurs/services, Spring l'envoie ici. La méthode
 * correspondante fabrique la réponse. Sans ça, le client recevrait une page
 * d'erreur HTML illisible.</p>
 *
 * <p>Exemple de réponse générée :
 * {@code {"type":".../errors/404","title":"Not Found","status":404,
 * "detail":"Utilisateur introuvable : id=5","timestamp":"..."}}</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Préfixe des URLs "type" du format RFC 7807 (documentation des erreurs). */
    private static final String TYPE_BASE = "https://enterprise.example.com/errors/";

    /**
     * Exceptions "prêtes à l'emploi" de Spring : on y précise le code HTTP
     * et le message (ex. ResponseStatusException(NOT_FOUND, "Utilisateur introuvable")).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        return base(ex.getStatusCode().value(), ex.getReason() != null ? ex.getReason() : ex.getMessage());
    }

    /**
     * Échec de validation Bean Validation (@NotBlank, @Email, @NotEmpty...).
     * Réponse 400 + détail champ par champ :
     * {@code "errors": {"email": "must not be blank"}}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST.value(), "Requête invalide");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalide" : fe.getDefaultMessage(),
                        (a, b) -> a)));
        return pd;
    }

    /**
     * Corps de requête ILLISIBLE : JSON mal formé, type de contenu incorrect...
     * C'est la faute du client → 400 (sans le handler, cette exception
     * tomberait dans le 500 générique, ce qui serait trompeur).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return base(HttpStatus.BAD_REQUEST.value(), "Corps de requête illisible : JSON invalide ou absent.");
    }

    /** Email ou mot de passe incorrect → 401 (sans préciser lequel, par sécurité). */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return base(HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage() != null ? ex.getMessage() : "Identifiants invalides.");
    }

    /** Authentifié mais pas les droits (ex. USER sur /api/users) → 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return base(403, "Droits insuffisants pour accéder à cette ressource.");
    }

    /** Token JWT absent/invalide sur une route protégée → 401. */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return base(401, "Authentification requise ou token invalide.");
    }

    /**
     * TOUTE AUTRE exception non prévue → 500 générique.
     * On ne renvoie JAMAIS le message technique au client (il pourrait
     * révéler des détails internes) ; il reste dans les logs du serveur.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        return base(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne du serveur");
    }

    /** Fabrique la structure JSON commune à toutes les erreurs. */
    private ProblemDetail base(int status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        pd.setType(URI.create(TYPE_BASE + status));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
