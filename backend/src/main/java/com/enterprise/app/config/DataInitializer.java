package com.enterprise.app.config;

import com.enterprise.app.user.Role;
import com.enterprise.app.user.RoleName;
import com.enterprise.app.user.RoleRepository;
import com.enterprise.app.user.User;
import com.enterprise.app.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Crée les rôles de base et un compte ADMIN au premier démarrage.
 *
 * <p>Identifiants par défaut : admin@enterprise.com / Admin123!
 * (à changer impérativement en production via les variables d'environnement).</p>
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    ApplicationRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               AppProperties appProperties) {
        return args -> {
            for (RoleName name : RoleName.values()) {
                roleRepository.findByName(name)
                        .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
            }

            if (userRepository.findByEmailIgnoreCase("admin@enterprise.com").isEmpty()) {
                Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
                User admin = User.builder()
                        .email("admin@enterprise.com")
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
