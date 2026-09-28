package com.enterprise.app.user;

import com.enterprise.app.auth.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Toute la LOGIQUE MÉTIER des comptes utilisateurs : lister, lire,
 * changer les rôles, activer/désactiver, supprimer.
 *
 * <p><b>C'est quoi un "service" ?</b> La couche où vit l'intelligence de
 * l'application. Le contrôleur (ci-dessous) reçoit les requêtes HTTP mais
 * ne décide de rien : il délègue au service. Le service, lui, parle à la
 * base via les repositories. Chaîne classique :
 * {@code Controller → Service → Repository → Base de données}.</p>
 */
@Service          // "Spring, crée UN SEUL objet de cette classe" (singleton)
@RequiredArgsConstructor  // Lombok : constructeur avec tous les champs "final"
public class UserService {

    /**
     * Les dépendances du service. Grâce à {@code @RequiredArgsConstructor}
     * (Lombok), pas besoin d'écrire le constructeur : Lombok génère
     * {@code UserService(UserRepository, RoleRepository)} et Spring y
     * injecte automatiquement ses propres instances (INJECTION DE DÉPENDANCES).
     * Le mot-clé "final" = "cette référence ne changera jamais" (bonne pratique).
     */
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Liste paginée des utilisateurs.
     *
     * @param page numéro de page (0 = première)
     * @param size nombre d'utilisateurs par page
     */
    @Transactional(readOnly = true)  // Lecture seule : Hibernate optimise
    public Page<UserResponse> findPage(int page, int size) {
        // PageRequest décrit "page n° X, taille Y, trié par id croissant".
        // .map(this::toResponse) transforme chaque User (entité) en
        // UserResponse (DTO) — on n'expose JAMAIS l'entité au client.
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()))
                .map(this::toResponse);
    }

    /**
     * Détail d'un utilisateur. 404 si l'id n'existe pas en base.
     */
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                // ResponseStatusException = manière simple de renvoyer un code
                // HTTP précis (ici 404 NOT FOUND) depuis un service.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id));
    }

    /**
     * Remplace les rôles d'un utilisateur par la liste fournie.
     * Chaque nom de rôle est validé : inconnu → 400 BAD REQUEST.
     */
    @Transactional  // Écriture : si une étape échoue, TOUT est annulé (rollback)
    public UserResponse updateRoles(Long id, List<String> roles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id));

        // Convertit ["USER", "MANAGER"] (textes) en objets Role (entités).
        Set<Role> newRoles = roles.stream()
                .map(name -> roleRepository.findByName(RoleName.valueOf(name))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle inconnu : " + name)))
                .collect(Collectors.toSet());

        user.setRoles(newRoles);
        return toResponse(userRepository.save(user));
    }

    /**
     * Active (true) ou désactive (false) un compte.
     * Un compte désactivé ne peut plus se connecter (voir AppUserDetailsService).
     */
    @Transactional
    public UserResponse updateEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id));
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    /**
     * Supprime un compte. 404 si l'utilisateur n'existe pas.
     */
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Convertit une entité User en DTO UserResponse (le "mapper").
     * Le mot de passe hashé n'apparaît PAS : jamais exposé au client.
     */
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isEnabled(),
                user.getRoles().stream().map(r -> r.getName().name()).toList(),
                user.getCreatedAt());
    }
}
