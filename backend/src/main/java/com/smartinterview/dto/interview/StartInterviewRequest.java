package com.smartinterview.dto.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO to start a new interview session.
 */
@Data
public class StartInterviewRequest {

    @NotBlank(message = "Session title is required")
    @Size(max = 200)
    private String sessionTitle;

    @Size(max = 200)
    private String targetRole;

    /** Optional: link a specific resume to this session */
    private Long resumeId;

    /** Optional: link a specific JD to this session */
    private Long jobDescriptionId;

    /** TECHNICAL | RESUME_BASED | JD_BASED | COMPREHENSIVE_ADAPTIVE */
    private String interviewType = "COMPREHENSIVE_ADAPTIVE";

    @Min(3) @Max(20)
    private int targetQuestionCount = 5;

    /** EASY | MEDIUM | HARD */
    private String difficulty = "MEDIUM";
}
