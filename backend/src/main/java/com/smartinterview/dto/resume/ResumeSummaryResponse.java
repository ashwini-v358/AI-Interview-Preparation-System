package com.smartinterview.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary of a previously uploaded resume (for listing endpoints).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSummaryResponse {
    private Long resumeId;
    private String originalFileName;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
    private boolean hasAnalysis;
    private List<String> topSkills;   // first 5 extracted skills
    private double experienceYears;
    private String summary;
}
