package com.enterprise.app.auth;

import com.enterprise.app.auth.dto.LoginRequest;
import com.enterprise.app.auth.dto.LoginResponse;
import com.enterprise.app.auth.dto.RegisterRequest;
import com.enterprise.app.auth.dto.UserResponse;
import com.enterprise.app.security.JwtService;
import com.enterprise.app.user.Role;
import com.enterprise.app.user.RoleName;
import com.enterprise.app.user.RoleRepository;
import com.enterprise.app.user.User;
import com.enterprise.app.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Inscription (rôle USER) et connexion (validation des identifiants + émission du JWT).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé : " + request.email());
        }
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Rôle USER manquant en base"));

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        User saved = userRepository.save(user);
        log.info("Nouvel utilisateur inscrit : {}", saved.getEmail());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));

        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getFullName(), roles);
        return new LoginResponse(token, "Bearer", user.getId(), user.getEmail(), user.getFullName(), roles);
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
