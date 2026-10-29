package com.smartinterview.dto.jd;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight summary DTO for the "My JDs" list view.
 */
@Data
@Builder
public class JdSummaryResponse {

    private Long jdId;
    private String title;
    private String companyName;
    private LocalDateTime uploadedAt;
    private boolean hasAnalysis;
    private List<String> topRequiredSkills;
    private double minimumExperienceYears;
    private String roleTitle;
}
