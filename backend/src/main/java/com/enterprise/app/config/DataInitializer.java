package com.enterprise.app.config;

import com.enterprise.app.user.Role;
import com.enterprise.app.user.RoleName;
import com.enterprise.app.user.RoleRepository;
import com.enterprise.app.user.User;
import com.enterprise.app.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Données de DÉMARRAGE ("seed") : au premier lancement, crée les rôles de
 * base et un compte ADMIN prêt à l'emploi.
 *
 * <p><b>C'est quoi ApplicationRunner ?</b> Un bout de code exécuté une fois,
 * juste après le démarrage complet de Spring. Idéal pour initialiser la base.
 * Le code est idempotent : le relancer 100 fois ne crée pas de doublons
 * (tout est vérifié avant insertion).</p>
 *
 * <p><b>Compte créé : admin@enterprise.com / Admin123!</b></p>
 *
 * <p><b>Bonne pratique sécurité :</b> ce compte de démonstration n'est créé
 * QUE si le profil actif n'est PAS "prod". En production, l'admin se crée
 * via une opération contrôlée (migration Flyway, script d'exploitation ou
 * variables d'environnement), jamais par un mot de passe écrit dans Git.
 * Les identifiants de démo sont également supprimés des logs en dehors
 * du développement.</p>
 */
@Configuration
@Slf4j  // Lombok : champ "log" généré
public class DataInitializer {

    /** Identifiants du compte de démonstration (hors production uniquement). */
    private static final String DEMO_ADMIN_EMAIL = "admin@enterprise.com";
    private static final String DEMO_ADMIN_PASSWORD = "Admin123!";

    /** Indique si le profil "prod" est actif (injecté par Spring). */
    private final Environment environment;

    public DataInitializer(Environment environment) {
        this.environment = environment;
    }

    /**
     * La méthode retourne une fonction (lambda) : Spring la stocke puis
     * l'exécute après le démarrage, avec la base prête.
     */
    @Bean
    ApplicationRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Crée chaque rôle de l'enum (ADMIN, MANAGER, USER) s'il n'existe pas.
            //    (Les rôles, eux, sont nécessaires dans TOUS les environnements.)
            for (RoleName name : RoleName.values()) {
                roleRepository.findByName(name)
                        // orElseGet : le rôle manque ? on le crée à la volée.
                        .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
            }

            // 2. Compte admin de démonstration : JAMAIS en production.
            if (isProdProfile()) {
                log.info("Profil prod actif : le compte admin de démonstration n'est pas créé.");
                return;
            }

            if (userRepository.findByEmailIgnoreCase(DEMO_ADMIN_EMAIL).isEmpty()) {
                Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
                User admin = User.builder()
                        .email(DEMO_ADMIN_EMAIL)
                        // Jamais de mot de passe en clair : hash BCrypt.
                        .password(passwordEncoder.encode(DEMO_ADMIN_PASSWORD))
                        .fullName("Administrateur")
                        .enabled(true)
                        .roles(Set.of(adminRole))
                        .build();
                userRepository.save(admin);
                // Le mot de passe n'apparaît PAS dans le log en dur : on ne
                // l'affiche qu'en développement (profil actif != test/prod).
                log.info("Compte ADMIN de démonstration créé : {}", DEMO_ADMIN_EMAIL);
            }
        };
    }

    /** Vrai si le profil "prod" est actif (SPRING_PROFILES_ACTIVE=prod). */
    private boolean isProdProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
