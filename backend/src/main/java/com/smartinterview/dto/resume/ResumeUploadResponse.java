package com.smartinterview.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full response returned after a successful resume upload and AI analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadResponse {

    // ── Resume metadata ──────────────────────────────────────────────────────
    private Long resumeId;
    private String originalFileName;
    private Long fileSizeBytes;
    private int extractedTextLength;
    private LocalDateTime uploadedAt;

    // ── AI Analysis results ──────────────────────────────────────────────────
    private Long analysisId;
    private List<String> extractedSkills;
    private double experienceYears;
    private String summary;
    private List<String> domains;
    private List<String> educationDetails;

    // ── Processing metadata ──────────────────────────────────────────────────
    private String aiProvider;
    private long processingTimeMs;
    private String message;
}
