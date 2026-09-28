package com.enterprise.app.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Transformation de toutes les erreurs en JSON RFC 7807 (Problem Detail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://enterprise.example.com/errors/";

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        return base(ex.getStatusCode().value(), ex.getReason() != null ? ex.getReason() : ex.getMessage());
    }

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

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return base(HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage() != null ? ex.getMessage() : "Identifiants invalides.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return base(403, "Droits insuffisants pour accéder à cette ressource.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return base(401, "Authentification requise ou token invalide.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        return base(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne du serveur");
    }

    private ProblemDetail base(int status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        pd.setType(URI.create(TYPE_BASE + status));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
