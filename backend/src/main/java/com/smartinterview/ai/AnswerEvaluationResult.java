package com.smartinterview.ai;

import java.util.List;

/**
 * Structured result of evaluating a candidate's answer.
 *
 * Scores are 0–10 floating-point values with the following semantics:
 *   0–3:  Poor / incomplete / off-topic
 *   4–6:  Adequate / partially correct
 *   7–8:  Good / mostly complete
 *   9–10: Excellent / comprehensive
 *
 * Scores use a 0–10 scale. Feedback and missing points are tied to the supplied
 * question rubric and interview context.
 */
public record AnswerEvaluationResult(
        double relevanceScore,
        double accuracyScore,
        double completenessScore,
        double clarityScore,
        double communicationScore,
        double depthScore,
        double overallScore,
        boolean answeredQuestion,
        List<String> strengths,
        List<String> missingPoints,
        String feedback,
        String suggestedImprovement,
        boolean followUpNeeded,
        String followUpTopic,
        String modelAnswer,
        String rawAiResponse
) {
    public AnswerEvaluationResult(
            double accuracyScore,
            double completenessScore,
            double clarityScore,
            double overallScore,
            String feedback,
            String modelAnswer,
            String rawAiResponse) {
        this(overallScore, accuracyScore, completenessScore, clarityScore, clarityScore,
                accuracyScore, overallScore, true, List.of(feedback), List.of(), feedback,
                feedback, false, null, modelAnswer, rawAiResponse);
    }
}
