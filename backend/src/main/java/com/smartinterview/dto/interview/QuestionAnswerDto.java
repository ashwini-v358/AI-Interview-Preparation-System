package com.smartinterview.dto.interview;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO pairing a question with the candidate's answer and evaluation scores.
 */
@Data
@Builder
public class QuestionAnswerDto {

    private Long questionId;
    private int questionOrder;
    private String questionText;
    private String difficultyLevel;

    // Answer details (null if not yet answered)
    private Long answerId;
    private String answerText;
    private Integer timeTakenSeconds;

    // Evaluation scores (null if not yet evaluated)
    private Long evaluationId;
    private Double technicalAccuracyScore;
    private Double completenessScore;
    private Double clarityScore;
    private Double overallScore;
    private String strengths;
    private String areasOfImprovement;
    private String detailedFeedback;
    private Double relevanceScore;
    private Double communicationScore;
    private Double depthScore;
    private Boolean answeredQuestion;
    private Boolean followUpNeeded;
    private List<String> strengthsList;
    private List<String> missingPoints;
    private String feedback;
    private String suggestedImprovement;
    private String followUpTopic;
    private List<AdaptiveReasonDto> selectionReasons;
}
