package com.smartinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object (DTO) for system health status.
 *
 * Concepts introduced:
 * - DTO (Data Transfer Object): An object specifically shaped to carry data
 *   between processes (client and server). Protects internal entity representations
 *   and limits exposed fields.
 * - Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor: Automatically
 *   generates getters, setters, toString, equals/hashCode, builder pattern, and constructors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private String application;
    private String version;
    private Instant timestamp;
    private Map<String, Object> details;
}
