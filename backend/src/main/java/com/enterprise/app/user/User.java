package com.enterprise.app.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * UTILISATEUR de l'application — c'est une "entité JPA".
 *
 * <p><b>C'est quoi une entité ?</b> Une classe Java mappée sur une table de la
 * base de données. Ici : la classe {@code User} ↔ la table {@code users}.
 * Chaque objet {@code User} créé en Java correspond à une ligne de la table,
 * et chaque attribut de la classe correspond à une colonne. Hibernate (le
 * moteur inclus dans Spring Data JPA) écrit automatiquement le SQL pour nous :
 * {@code save(user)} fait un INSERT/UPDATE, {@code findById(1L)} un SELECT...</p>
 *
 * <p><b>Les annotations Lombok en bas de classe évitent d'écrire à la main
 * tout le code répétitif</b> (getters, setters, constructeurs...) :
 * Lombok le GÉNÈRE à la compilation. Le fichier .class final contient
 * les méthodes, même si on ne les voit pas dans le code source.</p>
 */
@Entity                       // "Cette classe est une entité JPA"
@Table(name = "users")        // Nom de la table SQL (user est un mot réservé en SQL)
// ----- Annotations Lombok (génèrent le code répétitif) -----
@Getter                       // Génère getEmail(), getPassword(), etc.
@Setter                       // Génère setEmail(...), setPassword(...), etc.
@NoArgsConstructor            // Génère le constructeur sans arguments (obligatoire pour JPA)
@AllArgsConstructor           // Génère le constructeur avec TOUS les champs en paramètres
@Builder                      // Génère User.builder().email(...).build() (voir AuthService)
@ToString(exclude = "password") // Génère toString() SANS le mot de passe (sécurité)
@EqualsAndHashCode(of = "email") // Deux users sont "égaux" s'ils ont le même email
public class User {

    /**
     * Clé primaire : identifiant unique en base.
     * {@code @GeneratedValue(IDENTITY)} = c'est MySQL qui choisit le numéro
     * (1, 2, 3...) automatiquement à chaque insertion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /** Email de connexion — unique en base (deux comptes ne peuvent pas partager le même). */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Mot de passe HASHÉ avec BCrypt (jamais stocké en clair, jamais renvoyé
     * au client — c'est le rôle des DTO comme UserResponse).
     */
    @Column(nullable = false, length = 100)
    private String password;

    /** Nom complet affiché dans l'interface. */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Compte actif ou désactivé (un compte désactivé ne peut plus se connecter).
     * {@code = true} : valeur par défaut si on ne précise rien.
     * {@code @Builder.Default} : idem mais pour le pattern Builder Lombok —
     * sans ça, le builder ignorerait la valeur par défaut (warning détecté
     * lors de la compilation).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * RÔLES du compte (ADMIN, MANAGER, USER) — relation "plusieurs à plusieurs" :
     * un utilisateur a plusieurs rôles, un rôle est porté par plusieurs utilisateurs.
     *
     * <p>La table technique {@code users_roles} fait le lien (user_id, role_id) —
     * {@code @JoinTable} la décrit. {@code EAGER} = les rôles sont chargés en même
     * temps que l'utilisateur (on en a besoin à chaque connexion, autant les avoir).</p>
     *
     * <p>{@code new HashSet<>()} : un utilisateur commence sans rôles.
     * {@code @Builder.Default} s'applique aussi au builder.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /** Date de création — remplie AUTOMATIQUEMENT par Hibernate à l'insertion. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Date de dernière modification — mise à jour automatiquement à chaque save(). */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
