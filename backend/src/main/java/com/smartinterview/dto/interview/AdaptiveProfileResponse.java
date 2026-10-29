package com.smartinterview.dto.interview;

import java.time.LocalDateTime;
import java.util.List;

public record AdaptiveProfileResponse(
        boolean hasHistory,
        int completedInterviewCount,
        String suggestedDifficulty,
        List<SkillPerformance> skillProfile,
        List<InterviewTrend> history
) {
    public record SkillPerformance(
            String skillName,
            double scorePercent,
            double technicalAccuracyPercent,
            double completenessPercent,
            double clarityPercent,
            Double communicationPercent,
            String level,
            int previousMistakes
    ) {}

    public record InterviewTrend(
            Long sessionId,
            String sessionTitle,
            LocalDateTime completedAt,
            double overallScorePercent,
            List<SkillPerformance> skillScores
    ) {}
}