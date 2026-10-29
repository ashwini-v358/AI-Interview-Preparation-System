package com.smartinterview.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for both /register and /login — contains the JWT token
 * and basic user info for the frontend to store in state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type;
    private Long userId;
    private String email;
    private String fullName;
    private String role;

    public static AuthResponse of(String token, Long userId, String email, String fullName, String role) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .email(email)
                .fullName(fullName)
                .role(role)
                .build();
    }
}
