package com.smartinterview.ai;

import java.util.List;

/**
 * Structured question data returned by an AI provider.
 *
 * @param questionText       the question asked to the candidate.
 * @param category           provider's own grouping label, e.g. "Resume project".
 * @param targetSkill        the single skill this question assesses.
 * @param expectedKeyPoints  rubric used later to evaluate the candidate's answer.
 * @param groundingEvidence  exact candidate-source quotes the question relies on.
 * @param followUp           true when this question follows up on the previous answer.
 * @param questionType       one of TECHNICAL, PROJECT, SCENARIO, FOLLOW_UP, BEHAVIORAL, or null.
 * @param difficulty         the difficulty the provider calibrated to, or null.
 * @param reason             the "why this question" explanation, or null when the provider
 *                          supplies no rationale (the deterministic mock provider does not).
 */
public record InterviewQuestionResult(
        String questionText,
        String category,
        String targetSkill,
        List<String> expectedKeyPoints,
        List<String> groundingEvidence,
        boolean followUp,
        String questionType,
        String difficulty,
        String reason
) {
    public InterviewQuestionResult(
            String questionText,
            String category,
            String targetSkill,
            List<String> expectedKeyPoints,
            List<String> groundingEvidence,
            boolean followUp) {
        this(questionText, category, targetSkill, expectedKeyPoints, groundingEvidence, followUp, null, null, null);
    }
}
