package com.smartinterview.ai;

import java.util.List;

/** Structured final report. Report scores use a 0-100 scale. */
public record InterviewReportResult(
        double overallScore,
        double technicalScore,
        double communicationScore,
        double problemSolvingScore,
        double projectKnowledgeScore,
        double answerQualityScore,
        String executiveSummary,
        List<String> strengths,
        List<String> areasToImprove,
        List<Integer> strugglingQuestionOrders,
        List<String> recommendedTopics,
        List<String> improvementSuggestions,
        List<String> claimVerifications
) {}
