package com.enterprise.app.user;

import com.enterprise.app.auth.dto.UserResponse;
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
 * Gestion des comptes : listage paginé, détail, rôles, activation, suppression.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findPage(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id));
    }

    @Transactional
    public UserResponse updateRoles(Long id, List<String> roles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id));
        Set<Role> newRoles = roles.stream()
                .map(name -> roleRepository.findByName(RoleName.valueOf(name))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle inconnu : " + name)))
                .collect(Collectors.toSet());
        user.setRoles(newRoles);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id));
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : id=" + id);
        }
        userRepository.deleteById(id);
    }

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