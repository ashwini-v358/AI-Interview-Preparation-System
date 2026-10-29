package com.smartinterview.controller;

import com.smartinterview.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller providing system health check and environment status.
 *
 * Concepts introduced:
 * - @RestController: Marks this class as a Spring REST Controller where
 *   every method returns an Object that is automatically serialized into JSON.
 * - @RequestMapping: Sets the base URL path for all endpoints in this controller.
 * - Dependency Injection: The Environment bean is injected by Spring's IoC container.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System health and verification endpoints")
public class HealthController {

    private final Environment environment;

    @Value("${spring.application.name:smart-interview-backend}")
    private String applicationName;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    @Operation(summary = "Check backend service health", description = "Returns system status, active profiles, and timestamp")
    public ResponseEntity<HealthResponse> checkHealth() {
        Map<String, Object> details = new HashMap<>();
        details.put("activeProfiles", environment.getActiveProfiles());
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("osName", System.getProperty("os.name"));

        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .application(applicationName)
                .version("1.0.0")
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity.ok(response);
    }
}
