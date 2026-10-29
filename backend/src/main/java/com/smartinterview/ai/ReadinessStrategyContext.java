package com.smartinterview.ai;

import java.util.List;

public record ReadinessStrategyContext(
        String targetRole,
        String resumeFileName,
        String jobTitle,
        int overallReadiness,
        double skillMatchPercentage,
        double interviewPerformance,
        double technicalSkills,
        double problemSolving,
        double communicationClarity,
        double roleAlignment,
        double preparationProgress,
        List<String> strongSkills,
        List<String> improvementAreas,
        List<String> requiredSkills,
        List<String> criticalSkills,
        List<String> missingSkills,
        List<String> partialSkills
) {}
