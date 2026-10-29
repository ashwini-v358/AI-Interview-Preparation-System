package com.smartinterview.controller;

import com.smartinterview.dto.SchemaStatusResponse;
import com.smartinterview.service.SchemaStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing database verification and entity telemetry.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System Diagnostics", description = "Endpoints for database schema, health, and system inspection")
public class SchemaStatusController {

    private final SchemaStatusService schemaStatusService;

    @GetMapping("/schema-status")
    @Operation(summary = "Check Database & JPA Entity Status", description = "Returns live table record counts and JPA repository health.")
    public ResponseEntity<SchemaStatusResponse> getSchemaStatus() {
        return ResponseEntity.ok(schemaStatusService.getSchemaStatus());
    }
}
