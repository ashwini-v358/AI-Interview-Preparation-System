package com.smartinterview.service;

import com.smartinterview.dto.auth.*;
import com.smartinterview.entity.User;
import com.smartinterview.entity.enums.Role;
import com.smartinterview.exception.DuplicateResourceException;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.UserRepository;
import com.smartinterview.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService handles user registration, login, and profile retrieval.
 *
 * Concepts:
 * - AuthenticationManager.authenticate(): Delegates to DaoAuthenticationProvider,
 *   which loads the user from DB and verifies the BCrypt-hashed password.
 *   Throws BadCredentialsException if credentials don't match.
 * - SecurityContextHolder.setContext(): Stores auth state for the current thread
 *   so that downstream code can call getContext().getAuthentication() if needed.
 * - @Transactional: Ensures DB reads/writes are wrapped in a transaction.
 *   readOnly = true on read methods enables optimizations (no dirty check).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new CANDIDATE user.
     * Validates email uniqueness, hashes the password with BCrypt, persists,
     * then immediately issues a JWT so the user is logged in after registration.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with email '" + email + "' already exists.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.ROLE_CANDIDATE)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New candidate registered: {} (id={})", saved.getEmail(), saved.getId());

        String token = jwtTokenProvider.generateTokenFromEmail(saved.getEmail());
        return AuthResponse.of(token, saved.getId(), saved.getEmail(),
                saved.getFullName(), saved.getRole().name());
    }

    /**
     * Authenticates an existing user with email + password.
     * On success returns a fresh JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

        log.info("User logged in: {} (role={})", user.getEmail(), user.getRole());
        return AuthResponse.of(token, user.getId(), user.getEmail(),
                user.getFullName(), user.getRole().name());
    }

    /**
     * Returns the public profile of the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
