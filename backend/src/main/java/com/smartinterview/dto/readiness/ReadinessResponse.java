package com.smartinterview.dto.readiness;

import java.util.List;

public record ReadinessResponse(
        int overallReadiness,
        int technicalSkills,
        int problemSolving,
        int communicationClarity,
        int roleAlignment,
        int interviewPerformance,
        int preparationProgress,
        List<ReadinessSkill> strongSkills,
        List<ReadinessSkill> improvementAreas,
        List<ReadinessSkill> criticalSkills,
        List<ReadinessGoal> dailyGoals,
        List<ReadinessGoal> learningTopics,
        List<ReadinessGoal> practiceExercises,
        List<ReadinessGoal> interviewQuestionsToPractice,
        List<String> endOfDayChecklist,
        String nextRecommendedAction,
        String reasoning,
        List<ScoreBreakdown> scoreBreakdown
) {
    public record ReadinessSkill(
            String skillName,
            int currentScore,
            String why,
            boolean requiredByTargetRole,
            String priority,
            String evidence
    ) {}

    public record ReadinessGoal(
            String title,
            String description,
            String duration,
            List<String> details
    ) {}

    public record ScoreBreakdown(
            String metric,
            int score,
            String explanation
    ) {}
}
