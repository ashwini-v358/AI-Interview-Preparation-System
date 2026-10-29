package com.smartinterview.ai;

/**
 * A single piece of candidate-specific signal passed to the AI provider so a generated
 * interview question can be grounded in real evidence instead of generic knowledge prompts.
 *
 * Signals are grouped by origin in {@link InterviewContext} so the prompt can state the
 * candidate's situation explicitly:
 * - detected skill gaps        (level: MATCHED / WEAK / MISSING, evidence: the resume or JD wording)
 * - previous interview results (level: WEAK / MODERATE / STRONG, score: percent 0-100)
 * - preparation progress       (level: plan status such as IN_PROGRESS / COMPLETED, evidence: priority)
 *
 * @param skill         the skill name, always non-blank.
 * @param level         the classification for this signal, e.g. MISSING or STRONG.
 * @param scorePercent  prior performance 0-100 when a measured score exists, otherwise null.
 * @param evidence      short supporting text drawn from the candidate's own records.
 */
public record InterviewSkillSignal(
        String skill,
        String level,
        Double scorePercent,
        String evidence
) {
    public InterviewSkillSignal(String skill, String level) {
        this(skill, level, null, null);
    }

    public InterviewSkillSignal(String skill, String level, Double scorePercent) {
        this(skill, level, scorePercent, null);
    }
}
