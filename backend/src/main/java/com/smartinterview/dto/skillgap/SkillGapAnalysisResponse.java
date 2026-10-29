package com.smartinterview.dto.skillgap;

import java.time.LocalDateTime;
import java.util.List;

public record SkillGapAnalysisResponse(
        Long analysisId,
        Long resumeId,
        String resumeFileName,
        Long jobDescriptionId,
        String jobTitle,
        double matchPercentage,
        List<SkillItem> matchedSkills,
        List<SkillItem> partialSkills,
        List<SkillItem> missingSkills,
        List<String> recommendations,
        LocalDateTime analyzedAt
) {
    public record SkillItem(
            String skillName,
            String resumeEvidence,
            String jdRequirement,
            String suggestedPreparation
    ) {}
}