package com.enterprise.app.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * RÔLE métier persisté en base (ADMIN, MANAGER, USER) — seconde entité de la
 * relation plusieurs-à-plusieurs avec {@link User} (voir la table users_roles).
 *
 * <p><b>Pourquoi une table plutôt qu'une simple colonne ?</b> Ça permet d'ajouter
 * ou renommer un rôle sans toucher au code, et de faire des requêtes du genre
 * "tous les utilisateurs qui ont le rôle MANAGER".</p>
 */
@Entity
@Table(name = "roles")
// ----- Lombok : mêmes annotations que User (code répétitif généré) -----
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class Role {

    /** Clé primaire auto-générée par MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    /**
     * Nom du rôle, stocké sous forme de TEXTE lisible dans la base
     * ("ADMIN" et pas un code numérique illisible).
     *
     * <p>{@code @Enumerated(EnumType.STRING)} : convertit l'enum Java
     * {@link RoleName} en chaîne SQL (et inversement). Toujours préférer
     * STRING à ORDINAL (numéro) : si un jour on réordonne l'enum,
     * les données existantes resteraient cohérentes.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;
}
