package com.enterprise.app.user;

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
public record UpdateEnabledRequest(
        @NotNull Boolean enabled
) {
}
