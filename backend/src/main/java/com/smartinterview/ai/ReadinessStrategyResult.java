package com.smartinterview.ai;

import java.util.List;

public record ReadinessStrategyResult(
        int overallReadiness,
        List<ReadinessSkillScore> strongSkills,
        List<ReadinessSkillScore> improvementAreas,
        List<ReadinessSkillScore> criticalSkills,
        List<ReadinessGoal> dailyGoals,
        List<ReadinessGoal> learningTopics,
        List<ReadinessGoal> practiceExercises,
        List<ReadinessGoal> interviewQuestionsToPractice,
        List<String> endOfDayChecklist,
        String nextRecommendedAction,
        String reasoning,
        String rawAiResponse
) {
    public record ReadinessSkillScore(
            String skillName,
            int currentScore,
            String status,
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
}
