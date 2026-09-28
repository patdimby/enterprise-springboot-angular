package com.enterprise.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès aux données des UTILISATEURS (même principe que {@link RoleRepository} :
 * Spring Data génère le SQL à partir du nom des méthodes).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouve un utilisateur par email, en ignorant majuscules/minuscules
     * (Jean@Mail.com et jean@mail.com, c'est le même compte).
     * Utilisé par la connexion et par AppUserDetailsService.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Répond vrai/faux : est-ce que cet email est déjà pris ?
     * Génère "SELECT count(*) > 0 ..." — bien plus efficace que de
     * charger l'utilisateur entier juste pour vérifier son existence.
     * Utilisé à l'inscription pour refuser les doublons (409 Conflict).
     */
    boolean existsByEmailIgnoreCase(String email);
}
