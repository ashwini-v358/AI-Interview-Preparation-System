package com.smartinterview.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MockAIService — a deterministic, zero-dependency implementation of AIService.
 *
 * Purpose:
 * - Allows the full application to work end-to-end in local development
 *   and CI without any API keys or internet connection.
 * - Produces realistic-looking structured responses for all four AI operations.
 * - Active when `app.ai.provider=mock` (set in application.yml default).
 *
 * Concept — always registered as a bean:
 *   This provider is deterministic and needs no API key, so it is always available.
 *   It is the active provider when `app.ai.provider=mock` (the application.yml default),
 *   and it is the automatic fallback behind {@link FallbackAIService} when
 *   `app.ai.provider=gemini` and Gemini is unreachable or returns an unusable response.
 *   This is the Strategy pattern combined with a decorator for graceful degradation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockAIService implements AIService {

    private final ObjectMapper objectMapper;

    @Override
    public ResumeAnalysisResult analyzeResume(String rawResumeText) {
        log.info("[MockAI] analyzeResume called ({} chars)", rawResumeText.length());
        simulateLatency(200, 500);

        List<String> skills = detectSkillsFromText(rawResumeText);
        double experience = estimateExperience(rawResumeText);

        String rawJson = toJson(Map.of(
                "provider", "mock",
                "skills", skills,
                "experience_years", experience,
                "summary", "Experienced software engineer with a solid foundation in " + String.join(", ", skills.subList(0, Math.min(3, skills.size()))) + ".",
                "domains", List.of("Backend Development", "System Design"),
                "education", List.of("B.Tech in Computer Science")
        ));

        return new ResumeAnalysisResult(
                skills,
                experience,
                "Experienced software engineer with a solid foundation in " +
                        String.join(", ", skills.subList(0, Math.min(3, skills.size()))) + ".",
                List.of("Backend Development", "System Design"),
                List.of("B.Tech in Computer Science"),
                rawJson
        );
    }

    @Override
    public JobDescriptionAnalysisResult analyzeJobDescription(String rawJdText) {
        log.info("[MockAI] analyzeJobDescription called ({} chars)", rawJdText.length());
        simulateLatency(150, 400);

        List<String> required = List.of("Java", "Spring Boot", "REST APIs", "SQL", "Git");
        List<String> niceToHave = List.of("Kubernetes", "Kafka", "Microservices", "AWS");

        String rawJson = toJson(Map.of(
                "provider", "mock",
                "required_skills", required,
                "nice_to_have_skills", niceToHave,
                "role_title", extractRoleTitle(rawJdText),
                "experience_required", 2.5
        ));

        return new JobDescriptionAnalysisResult(
                required, niceToHave,
                extractRoleTitle(rawJdText),
                2.5,
                List.of("Backend Engineering", "Cloud"),
                rawJson
        );
    }

    @Override
    public String generateInterviewQuestion(InterviewContext context) {
        return generateInterviewQuestionResult(context).questionText();
        }

        @Override
        public InterviewQuestionResult generateInterviewQuestionResult(InterviewContext context) {
        log.info("[MockAI] generateInterviewQuestion Q{}/{}", context.questionNumber(), context.totalQuestions());
        simulateLatency(100, 300);

        String previousAnswer = last(context.previousAnswers());
        String previousQuestion = last(context.previousQuestions());
        AnswerEvaluationResult previousEvaluation = last(context.previousEvaluations());
        boolean shortAnswer = previousAnswer != null && previousAnswer.trim().split("\\s+").length < 28;
        if (context.focusSkill() != null && !context.focusSkill().isBlank()) {
            String skill = context.focusSkill();
            return new InterviewQuestionResult(
                "For " + skill + ", explain a practical implementation, the decisions you would make, "
                    + "and how you would verify the result.",
                "Adaptive focus", skill,
                List.of("Explain the core concept", "Describe an implementation", "Discuss a trade-off"),
                List.of(skill), false);
        }
        if (previousAnswer != null && (shortAnswer
            || (previousEvaluation != null && previousEvaluation.followUpNeeded()))) {
            String evidence = excerpt(previousAnswer, 140);
            String topic = previousEvaluation != null && previousEvaluation.followUpTopic() != null
                ? previousEvaluation.followUpTopic()
                : context.targetSkills().stream().findFirst().orElse("your technical approach");
            return new InterviewQuestionResult(
                "You mentioned \"" + evidence + "\". What specific technical decisions or trade-offs "
                    + "were involved, and why?",
                "Follow-up",
                topic,
                List.of("Explain the implementation details", "Give a concrete example", "Discuss a trade-off"),
                List.of(evidence),
                true);
        }

        if (!context.projectEvidence().isEmpty()) {
            String evidence = context.projectEvidence().get((context.questionNumber() - 1) % context.projectEvidence().size());
            String role = context.targetRole() == null ? "the target role" : context.targetRole();
            return new InterviewQuestionResult(
                "Your resume says \"" + evidence + "\". Walk me through the architecture and explain "
                    + "how that work demonstrates skills needed for " + role + ".",
                "Resume project",
                context.targetSkills().stream().findFirst().orElse("Project architecture"),
                List.of("Describe the components", "Explain data flow", "Justify a design decision"),
                List.of(evidence),
                false);
        }

        if (!context.jobRequiredSkills().isEmpty()) {
            String skill = context.jobRequiredSkills().get((context.questionNumber() - 1) % context.jobRequiredSkills().size());
            String role = context.targetRole() == null ? "this role" : context.targetRole();
            return new InterviewQuestionResult(
                "The job description emphasizes " + skill + ". For " + role
                    + ", how would you apply it to solve a realistic backend problem?",
                "Job requirement",
                skill,
                List.of("Explain the concept", "Describe an application", "Mention a trade-off"),
                List.of(skill),
                false);
        }

        if (!context.targetSkills().isEmpty()) {
            String skill = context.targetSkills().get((context.questionNumber() - 1) % context.targetSkills().size());
            return new InterviewQuestionResult(
                "Your resume lists " + skill + ". Describe a specific situation where you used it, "
                    + "including the problem and the result.",
                "Resume skill",
                skill,
                List.of("Describe the problem", "Explain your implementation", "State the result"),
                List.of(skill),
                false);
        }

        String role = context.targetRole() == null ? "this role" : context.targetRole();
        String focus = context.interviewType().contains("TECHNICAL")
            ? "a technical challenge" : "a relevant engineering challenge";
        return new InterviewQuestionResult(
            "For " + role + ", how would you approach " + focus
                + "? Explain your reasoning and how you would verify the result.",
            "Role fundamentals",
            "Problem solving",
            List.of("Clarify constraints", "Explain the approach", "Describe validation"),
            List.of(),
            false);
    }

    @Override
    public AnswerEvaluationResult evaluateAnswer(String question, String answer, InterviewContext context) {
        return evaluateAnswer(question, answer, context, List.of());
        }

        @Override
        public AnswerEvaluationResult evaluateAnswer(
            String question,
            String answer,
            InterviewContext context,
            List<String> expectedKeyPoints) {
        log.info("[MockAI] evaluateAnswer for Q{}", context.questionNumber());
        simulateLatency(200, 600);

        List<String> keyPoints = expectedKeyPoints == null ? List.of() : expectedKeyPoints;
        List<String> missing = keyPoints.stream().filter(point -> !covers(answer, point)).toList();
        int coveredCount = keyPoints.size() - missing.size();
        double relevance = score(5 + Math.min(5, overlapCount(answer, question) * 0.8));
        double completeness = keyPoints.isEmpty() ? score(4 + Math.min(6, answer.split("\\s+").length / 18.0))
            : score(10.0 * coveredCount / keyPoints.size());
        double accuracy = score(Math.min(10, 4 + coveredCount * 1.5 + overlapCount(answer, question) * 0.2));
        double clarity = score(Math.min(10, 5 + Math.min(4, answer.split("[.!?]+\\s*").length)));
        String lowerAnswer = answer.toLowerCase();
        double communication = score(Math.min(10, 5 + (lowerAnswer.contains("because") || lowerAnswer.contains("for example") ? 2 : 0)
            + (lowerAnswer.contains("first") || lowerAnswer.contains("then") ? 1 : 0)));
        double depth = score(Math.min(10, 4 + (lowerAnswer.matches(".*(trade-off|tradeoff|example|failure|scale|performance).*" ) ? 3 : 0)
            + Math.min(3, coveredCount)));
        boolean answered = !answer.isBlank() && answer.trim().split("\\s+").length >= 4;
        double overall = round(relevance * 0.15 + accuracy * 0.25 + completeness * 0.2
            + clarity * 0.1 + communication * 0.1 + depth * 0.2);
        List<String> strengths = new ArrayList<>();
        if (relevance >= 6) strengths.add("The answer addressed the question.");
        if (coveredCount > 0) strengths.add("Covered " + coveredCount + " expected point(s).");
        if (strengths.isEmpty()) strengths.add("The response provides a starting point for discussion.");
        boolean followUp = !answered || overall < 6.5 || !missing.isEmpty();
        String feedback = followUp
            ? "Your response needs a little more detail to fully address this question."
            : "Your response addressed the main points clearly.";
        String suggested = missing.isEmpty() ? "Add a concrete example or trade-off to deepen the answer."
            : "Consider covering: " + String.join("; ", missing) + ".";
        String modelAnswer = keyPoints.isEmpty() ? "" : String.join(". ", keyPoints);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", "mock");
        raw.put("relevanceScore", relevance);
        raw.put("accuracyScore", accuracy);
        raw.put("completenessScore", completeness);
        raw.put("clarityScore", clarity);
        raw.put("communicationScore", communication);
        raw.put("depthScore", depth);
        raw.put("overallScore", overall);
        raw.put("answeredQuestion", answered);
        raw.put("missingPoints", missing);
        raw.put("followUpNeeded", followUp);
        return new AnswerEvaluationResult(relevance, accuracy, completeness, clarity,
            communication, depth, overall, answered, strengths, missing, feedback,
            suggested, followUp, missing.stream().findFirst().orElse(null), modelAnswer, toJson(raw));
    }

    @Override
    public ReadinessStrategyResult generateReadinessStrategy(ReadinessStrategyContext context) {
        List<ReadinessStrategyResult.ReadinessSkillScore> strong = context.strongSkills().stream()
                .map(skill -> new ReadinessStrategyResult.ReadinessSkillScore(
                        skill,
                        82,
                        "Strong",
                        "Strong performance in prior interviews and present in your resume evidence.",
                        context.requiredSkills().contains(skill) || context.criticalSkills().contains(skill),
                        context.requiredSkills().contains(skill) ? "CRITICAL" : "GOOD",
                        "Resume evidence and interview history support this strength."
                )).toList();

        List<ReadinessStrategyResult.ReadinessSkillScore> weak = context.improvementAreas().stream()
                .map(skill -> new ReadinessStrategyResult.ReadinessSkillScore(
                        skill,
                        48,
                        "Improve",
                        "This area is required in the target role and has weaker preparation progress or interview performance.",
                        true,
                        "HIGH",
                        "Current preparation and interview results show this should be your next priority."
                )).toList();

        List<ReadinessStrategyResult.ReadinessSkillScore> critical = context.criticalSkills().stream()
                .map(skill -> new ReadinessStrategyResult.ReadinessSkillScore(
                        skill,
                        context.strongSkills().contains(skill) ? 80 : 49,
                        context.strongSkills().contains(skill) ? "Strong" : "Needs improvement",
                        context.strongSkills().contains(skill)
                                ? "This skill is already present and should be reinforced."
                                : "This required skill needs direct study and interview rehearsal.",
                        true,
                        context.strongSkills().contains(skill) ? "GOOD" : "CRITICAL",
                        context.strongSkills().contains(skill) ? "Demonstrated and consistent." : "Required role skill with current weakness."
                )).toList();

        List<String> checklist = List.of(
                "Completed today's learning topics",
                "Completed coding practice",
                "Practiced interview questions",
                "Revised weak concepts",
                "Completed one project explanation",
                "Took a short mock interview"
        );

        List<ReadinessStrategyResult.ReadinessGoal> dailyGoals = List.of(
                new ReadinessStrategyResult.ReadinessGoal(
                        "Priority 1",
                        weak.isEmpty() ? "Reinforce your strongest role-critical skills." : weak.get(0).skillName() + " readiness",
                        weak.isEmpty() ? "30 minutes" : "60 minutes",
                        weak.isEmpty()
                                ? List.of("Review your strongest role signals.", "Practice 2 short explanations.")
                                : List.of("Learn the core patterns for " + weak.get(0).skillName(), "Practice a focused exercise using that skill.")
                )
        );

        List<ReadinessStrategyResult.ReadinessGoal> learning = context.improvementAreas().stream()
                .limit(3)
                .map(skill -> new ReadinessStrategyResult.ReadinessGoal(
                        skill,
                        "Focus on fundamentals, application patterns, and trade-offs.",
                        "45 minutes",
                        List.of("Understand the key concepts", "Practice one applied example", "Explain the trade-offs clearly")
                )).toList();

        List<ReadinessStrategyResult.ReadinessGoal> practice = context.improvementAreas().stream()
                .limit(2)
                .map(skill -> new ReadinessStrategyResult.ReadinessGoal(
                        "Practice: " + skill,
                        "Build a small realistic exercise around the topic.",
                        "30-45 minutes",
                        List.of("Code a focused example", "Document the reasoning", "Explain the result clearly")
                )).toList();

        List<ReadinessStrategyResult.ReadinessGoal> questions = context.improvementAreas().stream()
                .limit(3)
                .map(skill -> new ReadinessStrategyResult.ReadinessGoal(
                        skill + " interview question",
                        "Prepare a scenario-based answer and explain the decision-making process.",
                        "15 minutes",
                        List.of("Describe the problem", "Explain your approach", "Discuss trade-offs and outcome")
                )).toList();

        String nextAction = weak.isEmpty()
                ? "Continue reinforcing your strongest skills and schedule one mock interview this week."
                : "Prioritize " + weak.get(0).skillName() + " today, then complete one practical exercise and one mock answer before your next interview.";

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("overallReadiness", Math.max(0, Math.min(100, (int) Math.round(context.skillMatchPercentage() * 0.4 + context.preparationProgress() * 0.2 + context.interviewPerformance() * 0.4))));
        raw.put("strongSkills", strong);
        raw.put("improvementAreas", weak);
        raw.put("criticalSkills", critical);
        raw.put("dailyGoals", dailyGoals);
        raw.put("learningTopics", learning);
        raw.put("practiceExercises", practice);
        raw.put("interviewQuestionsToPractice", questions);
        raw.put("endOfDayChecklist", checklist);
        raw.put("nextRecommendedAction", nextAction);
        raw.put("reasoning", "Readiness is calculated from role alignment, preparation progress, and observed interview performance.");

        return new ReadinessStrategyResult(
                Math.max(0, Math.min(100, (int) Math.round(context.skillMatchPercentage() * 0.4 + context.preparationProgress() * 0.2 + context.interviewPerformance() * 0.4))),
                strong,
                weak,
                critical,
                dailyGoals,
                learning,
                practice,
                questions,
                checklist,
                nextAction,
                "Readiness is based on role alignment, preparation progress, and previous interview performance to guide daily priorities.",
                toJson(raw)
        );
    }

    @Override
    public InterviewReportResult generateInterviewReport(InterviewReportContext context) {
        List<InterviewAnswerContext> answers = context.answers();
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("A completed interview needs evaluated answers to create a report");
        }
        List<AnswerEvaluationResult> evaluations = answers.stream()
            .map(InterviewAnswerContext::evaluation)
            .filter(java.util.Objects::nonNull)
            .toList();
        double overall = average(evaluations.stream().map(AnswerEvaluationResult::overallScore).toList()) * 10;
        double technical = average(evaluations.stream().map(AnswerEvaluationResult::accuracyScore).toList()) * 10;
        double communication = average(evaluations.stream().map(AnswerEvaluationResult::communicationScore).toList()) * 10;
        double problemSolving = average(evaluations.stream().map(AnswerEvaluationResult::depthScore).toList()) * 10;
        List<InterviewAnswerContext> projectAnswers = answers.stream()
            .filter(item -> item.questionText().toLowerCase().contains("project")
                || item.questionText().toLowerCase().contains("resume"))
            .toList();
        List<AnswerEvaluationResult> projectEvaluations = projectAnswers.stream()
            .map(InterviewAnswerContext::evaluation)
            .filter(java.util.Objects::nonNull)
            .toList();
        double projectKnowledge = projectEvaluations.isEmpty() ? overall
            : average(projectEvaluations.stream().map(AnswerEvaluationResult::accuracyScore).toList()) * 10;
        double answerQuality = average(evaluations.stream().map(AnswerEvaluationResult::overallScore).toList()) * 10;
        List<String> strengths = evaluations.stream().flatMap(item -> item.strengths().stream())
            .filter(value -> value != null && !value.isBlank()).distinct().limit(6).toList();
        List<String> missing = evaluations.stream().flatMap(item -> item.missingPoints().stream())
            .filter(value -> value != null && !value.isBlank()).distinct().limit(8).toList();
        List<Integer> struggled = answers.stream()
            .filter(item -> item.evaluation() != null && item.evaluation().overallScore() < 6.5)
            .map(InterviewAnswerContext::questionOrder)
            .toList();
        List<String> suggestions = missing.stream().map(item -> "Practice explaining " + item + " with a concrete example.")
            .limit(6).toList();
        String role = context.targetRole() == null ? "the target role" : context.targetRole();
        String summary = String.format("Completed %d evaluated answers for %s. Average answer quality was %.0f/100."
            , evaluations.size(), role, answerQuality);
        return new InterviewReportResult(round(overall), round(technical), round(communication),
            round(problemSolving), round(projectKnowledge), round(answerQuality), summary,
            strengths, missing, struggled, missing, suggestions, List.of());
        }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private List<String> detectSkillsFromText(String text) {
        String lower = text.toLowerCase();
        return List.of("Java", "Spring Boot", "REST APIs", "SQL", "Git",
                        "Docker", "AWS", "Microservices", "JPA/Hibernate", "React")
                .stream()
                .filter(skill -> lower.contains(skill.toLowerCase().split("/")[0]))
                .limit(8)
                .toList();
    }

    private double estimateExperience(String text) {
        // Rough heuristic: count "year" mentions near digits
        long mentions = text.chars().filter(c -> c == '\n').count();
        return Math.min(12.0, Math.max(1.0, Math.round(mentions / 15.0 * 10.0) / 10.0));
    }

    private String extractRoleTitle(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("senior")) return "Senior Software Engineer";
        if (lower.contains("lead")) return "Tech Lead";
        if (lower.contains("backend")) return "Backend Engineer";
        if (lower.contains("fullstack") || lower.contains("full stack")) return "Full Stack Developer";
        return "Software Engineer";
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private double average(List<Double> values) {
        return values == null || values.isEmpty() ? 0
                : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double score(double value) {
        return round(Math.max(0, Math.min(10, value)));
    }

    private <T> T last(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private boolean covers(String answer, String point) {
        return overlapCount(answer, point) >= Math.min(2, significantWords(point).size());
    }

    private int overlapCount(String answer, String reference) {
        List<String> answerWords = significantWords(answer);
        return (int) significantWords(reference).stream().filter(answerWords::contains).count();
    }

    private List<String> significantWords(String text) {
        return java.util.Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(word -> word.length() > 3)
                .distinct()
                .toList();
    }

    private String excerpt(String value, int maxLength) {
        String compact = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength) + "...";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
