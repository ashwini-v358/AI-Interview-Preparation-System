package com.smartinterview.dto.jd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for pasting a raw job description as text.
 */
@Data
public class JdUploadRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 150)
    private String companyName;

    @NotBlank(message = "Job description text is required")
    @Size(min = 50, message = "Job description must be at least 50 characters")
    private String rawText;
}
