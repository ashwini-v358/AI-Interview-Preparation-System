package com.smartinterview.ai;

/**
 * AIService — the central abstraction for all LLM interactions in the platform.
 *
 * Concepts:
 * - Interface Segregation: All controllers and services depend on THIS interface,
 *   not on any specific LLM provider. Swapping Gemini → OpenAI → Mock requires
 *   zero changes to business logic.
 * - The response format contract: all methods return well-defined Java records
 *   so downstream code can work with strongly-typed data, not raw JSON strings.
 *
 * Implementations:
 * - MockAIService  — deterministic responses, used for local dev and tests.
 * - GeminiAIService — Google Gemini Pro (Phase 8).
 * - OpenAIService  — OpenAI GPT-4o (configurable alternative).
 */
public interface AIService {

    /**
     * Analyses raw resume text and returns structured extraction results.
     *
     * @param rawResumeText the extracted plain text from a PDF resume.
     * @return {@link ResumeAnalysisResult} containing skills, experience, summary, domains.
     */
    ResumeAnalysisResult analyzeResume(String rawResumeText);

    /**
     * Analyses raw job description text and returns structured extraction.
     *
     * @param rawJdText the extracted plain text from a PDF or pasted JD.
     * @return {@link JobDescriptionAnalysisResult} with required skills, nice-to-haves.
     */
    JobDescriptionAnalysisResult analyzeJobDescription(String rawJdText);

    /**
     * Generates the next adaptive interview question given context.
     *
     * @param context the current interview context (skills, previous Q&A).
     * @return a generated question string.
     */
    String generateInterviewQuestion(InterviewContext context);

    default InterviewQuestionResult generateInterviewQuestionResult(InterviewContext context) {
        return new InterviewQuestionResult(generateInterviewQuestion(context), "Technical",
            context.focusSkill() != null ? context.focusSkill()
                : context.targetSkills().stream().findFirst().orElse("General"),
                java.util.List.of(), java.util.List.of(), false);
    }

    /**
     * Evaluates a candidate's answer to an interview question.
     *
     * @param question  the original question asked.
     * @param answer    the candidate's submitted answer.
     * @param context   interview context for calibration.
     * @return {@link AnswerEvaluationResult} with scores and feedback.
     */
    AnswerEvaluationResult evaluateAnswer(String question, String answer, InterviewContext context);

    default AnswerEvaluationResult evaluateAnswer(
            String question,
            String answer,
            InterviewContext context,
            java.util.List<String> expectedKeyPoints) {
        return evaluateAnswer(question, answer, context);
    }

    InterviewReportResult generateInterviewReport(InterviewReportContext context);

    default ReadinessStrategyResult generateReadinessStrategy(ReadinessStrategyContext context) {
        return new ReadinessStrategyResult(
                Math.max(0, Math.min(100, context.overallReadiness())),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                "Review your current gaps and keep practicing the weak role-critical skills.",
                "Deterministic default readiness strategy.",
                "{}"
        );
    }
}
