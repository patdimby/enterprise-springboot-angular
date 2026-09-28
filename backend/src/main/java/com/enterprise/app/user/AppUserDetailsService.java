package com.enterprise.app.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pont entre Spring Security et NOTRE table users.
 *
 * <p><b>À quoi ça sert ?</b> Au moment du login, Spring Security exécute
 * AuthenticationManager.authenticate(email, motDePasse). Pour vérifier le
 * mot de passe, il doit d'abord CHARGER le compte depuis la base : c'est
 * exactement le rôle de cette classe (méthode loadUserByUsername).</p>
 *
 * <p>Spring trouve tout seul cette classe (grâce à {@code @Service} et à
 * l'interface UserDetailsService) et l'utilise automatiquement — aucune
 * configuration supplémentaire à écrire.</p>
 */
@Service
@RequiredArgsConstructor  // Lombok : constructeur généré (injection du repository)
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Appelé par Spring Security pendant le login.
     *
     * @param email le "username" ici est l'email de connexion
     * @return le compte au format "UserDetails" (format interne Spring Security)
     * @throws UsernameNotFoundException si l'email est inconnu → login refusé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));

        // Convertit nos rôles (ADMIN, USER...) au format Spring Security :
        // "ROLE_ADMIN", "ROLE_USER" (préfixe ROLE_ obligatoire).
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();

        // Compte DÉSACTIVÉ si enabled == false : Spring Security refusera
        // alors le login avec "DisabledException", avant même de vérifier
        // le mot de passe.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())  // Le HASH : Spring comparera avec BCrypt
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }
}
