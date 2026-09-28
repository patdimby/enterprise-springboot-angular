package com.enterprise.app.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * IDENTITÉ de l'utilisateur, reconstruite depuis le JWT à chaque requête
 * (par {@link JwtAuthenticationFilter}) puis installée dans le SecurityContext.
 *
 * <p>Les contrôleurs la récupèrent avec {@code @AuthenticationPrincipal} —
 * voir AuthController.me(). C'est ce qui permet à {@code /api/auth/me} de
 * répondre "voilà qui tu es" sans reconsulter la base.</p>
 *
 * <p><b>Lombok utilisé ici</b> :</p>
 * <ul>
 *   <li>{@code @Value} = classe immuable : champs privés et finaux, getters
 *       uniquement, equals/hashCode/toString générés ;</li>
 *   <li>{@code @Builder} = fabrique objet lisible :
 *       {@code UserPrincipal.builder().id(1L).email("...").build()}.</li>
 * </ul>
 *
 * <p>Serializable = peut être converti en flux d'octets (utile si un jour
 * on stocke la session dans Redis ou autre).</p>
 */
@Value
@Builder
@Schema(description = "Identité courte de l'utilisateur, reconstruite depuis les "
        + "claims du JWT (réponse de GET /api/auth/me). Aucune lecture base : "
        + "les valeurs viennent uniquement du token.")
public class UserPrincipal implements Serializable {

    @Schema(description = "Identifiant technique du compte (claim 'id' du JWT).",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long id;

    @Schema(description = "Email du compte (claim 'sub' du JWT).",
            example = "admin@enterprise.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email;

    @Schema(description = "Nom complet (claim 'fullName' du JWT).",
            example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    String fullName;
}
