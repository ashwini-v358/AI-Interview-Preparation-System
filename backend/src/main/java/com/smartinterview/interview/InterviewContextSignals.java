package com.smartinterview.interview;

import com.smartinterview.ai.InterviewSkillSignal;

import java.util.List;

/**
 * Candidate-specific signals read from persisted records and handed to the AI provider so a
 * generated question is grounded in this candidate's real situation.
 *
 * @param skillGapSignals    detected MATCHED / WEAK / MISSING skills from the skill-gap analysis.
 * @param performanceSignals previous interview performance per skill, with measured scores.
 * @param preparationSignals skills currently covered by the candidate's preparation plan.
 */
public record InterviewContextSignals(
        List<InterviewSkillSignal> skillGapSignals,
        List<InterviewSkillSignal> performanceSignals,
        List<InterviewSkillSignal> preparationSignals
) {
    public static InterviewContextSignals empty() {
        return new InterviewContextSignals(List.of(), List.of(), List.of());
    }
}
