package com.smartinterview.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns HTTP 401 Unauthorized for requests that access protected resources
 * without a valid JWT token.
 *
 * Concept:
 * - AuthenticationEntryPoint: Spring Security's hook called when an unauthenticated
 *   request tries to access a secured resource. Without this, Spring would redirect
 *   to a login form — we return JSON 401 instead for REST APIs.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Unauthorized request to [{}]: {}", request.getRequestURI(), authException.getMessage());
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("""
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "Authentication required. Please provide a valid Bearer token.",
                  "path": "%s"
                }
                """.formatted(request.getRequestURI()));
    }
}
