package com.smartinterview.dto.preparation;

import java.time.LocalDateTime;
import java.util.List;

public record PreparationPlanResponse(
        Long planId,
        String title,
        Integer durationDays,
        LocalDateTime generatedAt,
        Long skillGapAnalysisId,
        String resumeFileName,
        String jobTitle,
        double matchPercentage,
        List<String> strongSkills,
        List<SkillPlanItem> skillAreas
) {
    public record SkillPlanItem(
            String skillName,
            String currentStatus,
            String priority,
            Integer estimatedDays,
            Integer estimatedHours,
            List<String> recommendedTopics,
            List<String> practiceTasks,
            List<String> interviewSuggestions
    ) {}
}