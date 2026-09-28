package com.enterprise.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès aux données des RÔLES — c'est un "repository".
 *
 * <p><b>C'est quoi un repository ?</b> Une interface Spring Data qui génère
 * TOUTE la mécanique SQL pour nous. On écrit juste le nom de la méthode :
 * {@code findByName(...)} → Spring génère "SELECT * FROM roles WHERE name = ?".
 * Pas une ligne de SQL !</p>
 *
 * <p>{@code JpaRepository<Role, Long>} : "Role" = l'entité gérée,
 * "Long" = le type de sa clé primaire. En héritant, on reçoit gratuitement
 * save(), findById(), findAll(), deleteById()... sans rien écrire.</p>
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Cherche un rôle par son nom.
     * Spring lit le nom de la méthode : "findBy" + "Name" → WHERE name = ?.
     * {@code Optional} = "peut-être qu'il n'existe pas" : on évite ainsi
     * le fameux NullPointerException.
     */
    Optional<Role> findByName(RoleName name);
}
