package com.smartinterview.dto.jd;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO returned after uploading and analysing a job description.
 */
@Data
@Builder
public class JdUploadResponse {

    private Long jdId;
    private String title;
    private String companyName;
    private int rawTextLength;
    private LocalDateTime uploadedAt;

    // AI Analysis results
    private Long analysisId;
    private String roleTitle;
    private List<String> requiredSkills;
    private List<String> niceToHaveSkills;
    private double minimumExperienceYears;
    private List<String> domains;
    private String aiProvider;
    private long processingTimeMs;
    private String message;
}
