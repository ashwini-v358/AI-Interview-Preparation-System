package com.smartinterview.dto.skillgap;

import jakarta.validation.constraints.NotNull;

public record SkillGapAnalysisRequest(
        @NotNull Long resumeId,
        @NotNull Long jobDescriptionId
) {}