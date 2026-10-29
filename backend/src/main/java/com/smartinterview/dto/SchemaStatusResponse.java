package com.smartinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Diagnostic payload reporting the operational status and table counts of JPA entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaStatusResponse {
    private String databaseEngine;
    private boolean jpaInitialized;
    private long totalUsers;
    private long totalResumes;
    private long totalJobDescriptions;
    private long totalSkills;
    private long totalInterviewSessions;
    private Map<String, Long> entityCounts;
    private LocalDateTime timestamp;
    private String phase;
    private String message;
}
