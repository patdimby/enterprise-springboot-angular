package com.enterprise.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Requête d'activation/désactivation d'un compte.
 *
 * <p><b>C'est quoi un "record" Java ?</b> Une classe ultra-compacte pour porter
 * des données : le constructeur, les accesseurs, equals/hashCode sont générés
 * automatiquement. Parfait pour les DTO (objets qui voyagent en JSON).
 * Exemple de JSON reçu : {@code {"enabled": false}}</p>
 *
 * <p>{@code @NotNull} = validation Bean Validation : si le champ manque dans
 * le JSON, Spring renvoie automatiquement un 400 avec le détail de l'erreur
 * (voir GlobalExceptionHandler).</p>
 */
@Schema(description = "Nouvel état d'activité du compte "
        + "(PATCH /api/users/{id}/enabled).")
public record UpdateEnabledRequest(
        @Schema(description = "true = compte actif (peut se connecter) ; "
                + "false = compte désactivé (connexion refusée).",
                example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean enabled
) {
}
