package com.smartinterview.ai;

import java.util.List;

/**
 * Context passed to the AI for question generation and answer evaluation.
 * Carries everything the model needs to produce calibrated, relevant output.
 *
 * @param interviewType         The type of interview (TECHNICAL, RESUME_BASED, etc.).
 * @param targetRole            The role being interviewed for, when available.
 * @param targetSkills          Skills the interview should focus on.
 * @param difficultyLevel       Desired difficulty (EASY, MEDIUM, HARD).
 * @param previousQuestions     Questions already asked in this session.
 * @param previousAnswers       Candidate answers to previousQuestions.
 * @param previousEvaluations   Evaluations corresponding to the previous answers.
 * @param resumeSummary         Brief candidate summary from resume analysis.
 * @param resumeText            Source resume text used to ground project questions.
 * @param projectEvidence       Project excerpts taken from the resume source text.
 * @param experienceYears       Experience from resume analysis, when available.
 * @param jobDescriptionTitle   Stored JD title.
 * @param jobDescriptionText    Source job-description text.
 * @param jobRequiredSkills     Required skills extracted from the JD.
 * @param questionNumber        Current question index (1-based).
 * @param totalQuestions        Total questions planned for the session.
 * @param focusSkill            Skill that an adaptive question should specifically target.
 * @param skillGapSignals       Detected skill gaps (MATCHED / WEAK / MISSING) for this candidate.
 * @param performanceSignals    Previous interview performance per skill, with measured scores.
 * @param preparationSignals    Preparation-plan progress per skill.
 * @param selectionRationales   Human-readable reasons the platform picked the current focus skill.
 */
public record InterviewContext(
        String interviewType,
        String targetRole,
        List<String> targetSkills,
        String difficultyLevel,
        List<String> previousQuestions,
        List<String> previousAnswers,
        List<AnswerEvaluationResult> previousEvaluations,
        String resumeSummary,
        String resumeText,
        List<String> projectEvidence,
        Double experienceYears,
        String jobDescriptionTitle,
        String jobDescriptionText,
        List<String> jobRequiredSkills,
        int questionNumber,
        int totalQuestions,
        String focusSkill,
        List<InterviewSkillSignal> skillGapSignals,
        List<InterviewSkillSignal> performanceSignals,
        List<InterviewSkillSignal> preparationSignals,
        List<String> selectionRationales
) {
    public InterviewContext(
                        String interviewType,
                        String targetRole,
                        List<String> targetSkills,
                        String difficultyLevel,
                        List<String> previousQuestions,
                        List<String> previousAnswers,
                        List<AnswerEvaluationResult> previousEvaluations,
                        String resumeSummary,
                        String resumeText,
                        List<String> projectEvidence,
                        Double experienceYears,
                        String jobDescriptionTitle,
                        String jobDescriptionText,
                        List<String> jobRequiredSkills,
                        int questionNumber,
                        int totalQuestions,
                        String focusSkill) {
        this(interviewType, targetRole, targetSkills, difficultyLevel, previousQuestions, previousAnswers,
                        previousEvaluations, resumeSummary, resumeText, projectEvidence, experienceYears,
                        jobDescriptionTitle, jobDescriptionText, jobRequiredSkills, questionNumber, totalQuestions,
                        focusSkill, List.of(), List.of(), List.of(), List.of());
    }

    public InterviewContext(
                        String interviewType,
                        String targetRole,
                        List<String> targetSkills,
                        String difficultyLevel,
                        List<String> previousQuestions,
                        List<String> previousAnswers,
                        List<AnswerEvaluationResult> previousEvaluations,
                        String resumeSummary,
                        String resumeText,
                        List<String> projectEvidence,
                        Double experienceYears,
                        String jobDescriptionTitle,
                        String jobDescriptionText,
                        List<String> jobRequiredSkills,
                        int questionNumber,
                        int totalQuestions) {
        this(interviewType, targetRole, targetSkills, difficultyLevel, previousQuestions, previousAnswers,
                        previousEvaluations, resumeSummary, resumeText, projectEvidence, experienceYears,
                        jobDescriptionTitle, jobDescriptionText, jobRequiredSkills, questionNumber, totalQuestions, null);
    }

    public InterviewContext(
            String interviewType,
            List<String> targetSkills,
            String difficultyLevel,
            List<String> previousQuestions,
            List<String> previousAnswers,
            String resumeSummary,
            int questionNumber,
            int totalQuestions) {
        this(interviewType, null, targetSkills, difficultyLevel, previousQuestions, previousAnswers,
                List.of(), resumeSummary, null, List.of(), null, null, null, List.of(),
                questionNumber, totalQuestions, null);
    }
}
