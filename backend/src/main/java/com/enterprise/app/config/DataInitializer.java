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
 * <p>Compte créé : admin@enterprise.com / Admin123! (à changer en prod !).</p>
 */
@Configuration
@Slf4j  // Lombok : champ "log" généré
public class DataInitializer {

    /**
     * La méthode retourne une fonction (lambda) : Spring la stocke puis
     * l'exécute après le démarrage, avec la base prête.
     */
    @Bean
    ApplicationRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               AppProperties appProperties) {
        return args -> {
            // 1. Crée chaque rôle de l'enum (ADMIN, MANAGER, USER) s'il n'existe pas.
            for (RoleName name : RoleName.values()) {
                roleRepository.findByName(name)
                        // orElseGet : le rôle manque ? on le crée à la volée.
                        .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
            }

            // 2. Crée le compte admin s'il n'existe pas encore.
            if (userRepository.findByEmailIgnoreCase("admin@enterprise.com").isEmpty()) {
                Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
                User admin = User.builder()
                        .email("admin@enterprise.com")
                        // Jamais de mot de passe en clair : hash BCrypt.
                        .password(passwordEncoder.encode("Admin123!"))
                        .fullName("Administrateur")
                        .enabled(true)
                        .roles(Set.of(adminRole))
                        .build();
                userRepository.save(admin);
                log.info("Compte ADMIN initial créé : admin@enterprise.com / Admin123!");
            }
        };
    }
}
