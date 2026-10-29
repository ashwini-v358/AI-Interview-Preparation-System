package com.smartinterview.dto.interview;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InterviewReportResponse {
    private Long reportId;
    private Long sessionId;
    private double overallScore;
    private double technicalScore;
    private double communicationScore;
    private double problemSolvingScore;
    private double projectKnowledgeScore;
    private double answerQualityScore;
    private String executiveSummary;
    private List<String> strengths;
    private List<String> areasToImprove;
    private List<Integer> strugglingQuestionOrders;
    private List<String> recommendedTopics;
    private List<String> improvementSuggestions;
    private List<String> claimVerifications;
    private LocalDateTime generatedAt;
}
