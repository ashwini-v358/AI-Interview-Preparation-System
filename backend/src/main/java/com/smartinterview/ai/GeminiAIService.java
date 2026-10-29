package com.smartinterview.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * GeminiAIService — real Google Gemini generation for every AIService operation.
 *
 * Responsibilities:
 * - Build a strongly grounded interviewer prompt from {@link InterviewContext}.
 * - Ask Gemini for a structured JSON payload and validate it before returning.
 * - Fail with a typed exception ({@link AIProviderException} for transport problems,
 *   {@link AIResponseValidationException} for unusable payloads) so callers — notably
 *   {@link FallbackAIService} — can degrade instead of surfacing an error to the candidate.
 *
 * This service never logs the API key and never starts the application in a broken state:
 * a missing key surfaces as an {@link AIProviderException} on first use, which the fallback
 * decorator converts into a working mock interview.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiAIService implements AIService {

    private static final Set<String> QUESTION_TYPES =
            Set.of("TECHNICAL", "PROJECT", "SCENARIO", "FOLLOW_UP", "BEHAVIORAL");
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiAIService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-2.0-flash}") String model,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public ResumeAnalysisResult analyzeResume(String rawResumeText) {
        String json = generateJson("""
                Analyze the supplied resume text. Extract only information explicitly present.
                Do not infer or invent experience, projects, education, or skills.
                Return JSON with exactly these fields: extractedSkills (string array),
                experienceYears (number), summary (string), domains (string array),
                educationDetails (string array).
                Resume text:
                %s
                """.formatted(rawResumeText));
        ResumeAnalysisResult result = readPayload(json, ResumeAnalysisResult.class,
                "Gemini returned invalid resume-analysis JSON");
        if (result.extractedSkills() == null || result.summary() == null
                || result.domains() == null || result.educationDetails() == null
                || !Double.isFinite(result.experienceYears()) || result.experienceYears() < 0) {
            throw new AIResponseValidationException("Gemini returned incomplete resume analysis");
        }
        return new ResumeAnalysisResult(result.extractedSkills(), result.experienceYears(),
                result.summary(), result.domains(), result.educationDetails(), json);
    }

    @Override
    public JobDescriptionAnalysisResult analyzeJobDescription(String rawJdText) {
        String json = generateJson("""
                Analyze the supplied job description. Return only information stated or directly
                required by it. Return JSON with exactly these fields: requiredSkills (string array),
                niceToHaveSkills (string array), roleTitle (string), experienceRequired (number),
                domains (string array).
                Job description:
                %s
                """.formatted(rawJdText));
        JobDescriptionAnalysisResult result = readPayload(json, JobDescriptionAnalysisResult.class,
                "Gemini returned invalid job-description JSON");
        if (result.requiredSkills() == null || result.niceToHaveSkills() == null
                || result.roleTitle() == null || result.domains() == null
                || !Double.isFinite(result.experienceRequired()) || result.experienceRequired() < 0) {
            throw new AIResponseValidationException("Gemini returned incomplete job-description analysis");
        }
        return new JobDescriptionAnalysisResult(result.requiredSkills(), result.niceToHaveSkills(),
                result.roleTitle(), result.experienceRequired(), result.domains(), json);
    }

    @Override
    public String generateInterviewQuestion(InterviewContext context) {
        return generateInterviewQuestionResult(context).questionText();
    }

    @Override
    public InterviewQuestionResult generateInterviewQuestionResult(InterviewContext context) {
        String contextJson = toJson(context);
        String json = generateJson(buildInterviewQuestionPrompt(contextJson));
        QuestionPayload payload = readPayload(json, QuestionPayload.class,
                "Gemini returned invalid question JSON");
        validateQuestion(payload, context);
        return new InterviewQuestionResult(payload.question(),
                valueOr(payload.category(), questionTypeOrCategory(payload)),
                valueOr(payload.skill(), "General"),
                payload.expectedKeyPoints(),
                payload.groundingEvidence(),
                Boolean.TRUE.equals(payload.followUp()),
                payload.questionType(),
                payload.difficulty(),
                valueOr(payload.reason(), null));
    }

    /**
     * Builds the interviewer prompt. The candidate context is injected as JSON so the model
     * can distinguish job requirements, resume evidence, detected gaps, prior performance and
     * preparation progress instead of guessing what matters.
     */
    private String buildInterviewQuestionPrompt(String contextJson) {
        return """
                You are a REAL technical interviewer running a live, adaptive mock interview.
                Generate exactly ONE next question for the candidate described by the context JSON below.

                The interview must feel continuous and specific to this person, not a generic quiz.

                Choose the subject of the question using this priority order:
                1. Requirements in the target job description.
                2. The candidate's weak or missing skills (skillGapSignals).
                3. Concrete experience in the candidate's resume and projects.
                4. Weaknesses from previous interview answers and past sessions (performanceSignals).
                5. Preparation progress (preparationSignals).
                6. The current difficultyLevel and questionNumber progression.
                7. previousQuestions, which you must NOT repeat or paraphrase.

                How to write the question:
                - Anchor it in the candidate's actual context. Reference their own project, stack, or
                  role requirement by name instead of asking a textbook question.
                - Prefer a concrete situation with a real failure mode, scale problem, or design
                  trade-off that the candidate would plausibly face on this job.
                - Match the current difficultyLevel. EASY checks understanding, MEDIUM checks applied
                  judgement, HARD checks trade-offs under constraints.
                - If the last answer was weak, incomplete, or a follow-up was requested, drill into that
                  same area and set questionType to FOLLOW_UP. If the last answer was strong, move to a
                  different relevant area.
                - Keep it answerable verbally in two to four sentences. Ask one thing at a time.
                - Do NOT ask generic questions such as "What is Java?" or "What is Spring Boot?" unless the
                  candidate's context genuinely makes that the right next question.
                - Never state a fact about the candidate that is not supported by the context.

                Set `reason` to one sentence explaining why THIS question, for THIS candidate, right now,
                naming the real signal that drove it (for example a weak prior score, a required job skill,
                a skill gap, or incomplete preparation). Do not invent a justification.

                Return ONLY a JSON object with exactly these fields:
                {
                  "question": string,
                  "skill": string,
                  "difficulty": "EASY"|"MEDIUM"|"HARD",
                  "questionType": "TECHNICAL"|"PROJECT"|"SCENARIO"|"FOLLOW_UP"|"BEHAVIORAL",
                  "reason": string,
                  "expectedKeyPoints": [string],
                  "groundingEvidence": [string],
                  "followUp": boolean
                }
                `expectedKeyPoints` lists 2-4 concrete points a strong answer would cover.
                `groundingEvidence` lists the exact short quotes from the context that the question relies
                on; it must be an empty array for a general role question that needs no candidate-specific
                premise, and any candidate-specific premise in `question` requires a quote here.
                `followUp` is true only when the question directly continues the previous answer.

                Interview context JSON:
                %s
                """.formatted(contextJson);
    }

    private void validateQuestion(QuestionPayload result, InterviewContext context) {
        if (result.question() == null || result.question().isBlank()
                || result.question().length() > 1000
                || result.expectedKeyPoints() == null || result.expectedKeyPoints().isEmpty()
                || result.groundingEvidence() == null) {
            throw new AIResponseValidationException("Gemini returned an incomplete interview question");
        }
        if (result.questionType() != null && !QUESTION_TYPES.contains(result.questionType())) {
            throw new AIResponseValidationException("Gemini returned an unsupported question type");
        }
        if (result.difficulty() != null && !DIFFICULTIES.contains(result.difficulty())) {
            throw new AIResponseValidationException("Gemini returned an unsupported difficulty level");
        }
        validateGrounding(result, context);
    }

    private void validateGrounding(QuestionPayload result, InterviewContext context) {
        String source = groundingSource(context);
        for (String evidence : result.groundingEvidence()) {
            if (evidence == null || evidence.isBlank() || !source.contains(evidence.toLowerCase())) {
                throw new AIResponseValidationException("Gemini question included unsupported grounding evidence");
            }
        }
        String question = result.question().toLowerCase();
        boolean claimsResumeFact = question.contains("your project") || question.contains("your resume")
                || question.contains("you built") || question.contains("you developed");
        if (claimsResumeFact && result.groundingEvidence().isEmpty()) {
            throw new AIResponseValidationException("Gemini question made an ungrounded candidate-specific claim");
        }
    }

    private String groundingSource(InterviewContext context) {
        Set<String> parts = new LinkedHashSet<>();
        parts.add(nonNull(context.resumeText()));
        parts.add(nonNull(context.resumeSummary()));
        parts.add(nonNull(context.jobDescriptionTitle()));
        parts.add(nonNull(context.jobDescriptionText()));
        parts.addAll(safeList(context.projectEvidence()));
        parts.addAll(safeList(context.previousAnswers()));
        parts.addAll(safeList(context.previousQuestions()));
        parts.addAll(safeList(context.targetSkills()));
        parts.addAll(safeList(context.jobRequiredSkills()));
        parts.addAll(safeList(context.selectionRationales()));
        parts.addAll(signalEvidence(context.skillGapSignals()));
        parts.addAll(signalEvidence(context.performanceSignals()));
        parts.addAll(signalEvidence(context.preparationSignals()));
        return String.join("\n", parts).toLowerCase(Locale.ROOT);
    }

    private List<String> signalEvidence(List<InterviewSkillSignal> signals) {
        return safeList(signals).stream()
                .flatMap(signal -> java.util.stream.Stream.of(signal.skill(), signal.evidence()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String questionTypeOrCategory(QuestionPayload payload) {
        if (payload.questionType() != null && !payload.questionType().isBlank()) return payload.questionType();
        if (payload.difficulty() != null && !payload.difficulty().isBlank()) return payload.difficulty() + " technical";
        return "Technical";
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
        String evaluationContext = toJson(Map.of(
                "interview", context,
                "expectedKeyPoints", expectedKeyPoints == null ? List.of() : expectedKeyPoints,
                "question", question,
                "answer", answer));
        String json = generateJson("""
                Evaluate only the candidate's answer to the current question. Score each dimension
                from 0 through 10. Do not award credit for points the answer does not make. Compare
                against expectedKeyPoints when present. Return JSON with exactly these fields:
                relevanceScore (number), accuracyScore (number), completenessScore (number),
                clarityScore (number), communicationScore (number), depthScore (number),
                overallScore (number), answeredQuestion (boolean), strengths (string array),
                missingPoints (string array), feedback (string), suggestedImprovement (string),
                followUpNeeded (boolean), followUpTopic (string), modelAnswer (string).
                Interview and answer JSON:
                %s
                """.formatted(evaluationContext));
        EvaluationPayload result = readPayload(json, EvaluationPayload.class,
                "Gemini returned invalid answer-evaluation JSON");
        validateEvaluation(result);
        return new AnswerEvaluationResult(result.relevanceScore(), result.accuracyScore(),
                result.completenessScore(), result.clarityScore(), result.communicationScore(),
                result.depthScore(), result.overallScore(), result.answeredQuestion(),
                result.strengths(), result.missingPoints(), result.feedback(),
                result.suggestedImprovement(), result.followUpNeeded(), result.followUpTopic(),
                result.modelAnswer(), json);
    }

    @Override
    public InterviewReportResult generateInterviewReport(InterviewReportContext context) {
        String json = generateJson("""
                Create a final interview report based only on the supplied actual questions, answers,
                expected points, evaluations, resume text, and job description. Do not invent candidate
                achievements or external verification. Report scores from 0 through 100. Return JSON
                with exactly these fields: overallScore, technicalScore, communicationScore,
                problemSolvingScore, projectKnowledgeScore, answerQualityScore (numbers),
                executiveSummary (string), strengths (string array), areasToImprove (string array),
                strugglingQuestionOrders (integer array), recommendedTopics (string array),
                improvementSuggestions (string array), claimVerifications (string array).
                Report context JSON:
                %s
                """.formatted(toJson(context)));
        ReportPayload result = readPayload(json, ReportPayload.class,
                "Gemini returned invalid interview-report JSON");
        validateReport(result, context);
        return new InterviewReportResult(result.overallScore(), result.technicalScore(),
                result.communicationScore(), result.problemSolvingScore(), result.projectKnowledgeScore(),
                result.answerQualityScore(), result.executiveSummary(), result.strengths(),
                result.areasToImprove(), result.strugglingQuestionOrders(), result.recommendedTopics(),
                result.improvementSuggestions(), result.claimVerifications());
    }

    private String generateJson(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("Gemini is selected as the AI provider but no API key is configured");
        }
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.4));
        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode text = response == null ? null
                    : response.at("/candidates/0/content/parts/0/text");
            if (text == null || !text.isTextual() || text.asText().isBlank()) {
                throw new AIResponseValidationException("Gemini response did not contain JSON text");
            }
            return text.asText();
        } catch (AIResponseValidationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AIProviderException("Gemini request failed: " + safeMessage(ex), ex);
        }
    }

    /**
     * Parses a Gemini payload, tolerating a markdown code fence around the JSON object and
     * unknown extra fields. Anything that is still not usable JSON becomes a validation
     * failure so the caller can fall back instead of crashing the interview.
     */
    private <T> T readPayload(String json, Class<T> type, String errorMessage) {
        try {
            return objectMapper.readValue(stripCodeFence(json), type);
        } catch (JsonProcessingException ex) {
            throw new AIResponseValidationException(errorMessage, ex);
        }
    }

    private String stripCodeFence(String json) {
        if (json == null) return "";
        String trimmed = json.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) return trimmed;
        String body = trimmed.substring(firstNewline + 1);
        int closingFence = body.lastIndexOf("```");
        if (closingFence < 0) return body.trim();
        return body.substring(0, closingFence).trim();
    }

    private void validateEvaluation(EvaluationPayload result) {
        if (result.relevanceScore() == null || result.accuracyScore() == null
                || result.completenessScore() == null || result.clarityScore() == null
                || result.communicationScore() == null || result.depthScore() == null
                || result.overallScore() == null || result.answeredQuestion() == null
                || result.strengths() == null || result.missingPoints() == null
                || result.feedback() == null || result.suggestedImprovement() == null
                || result.followUpNeeded() == null) {
            throw new AIResponseValidationException("Gemini returned incomplete answer evaluation");
        }
        for (Double score : List.of(result.relevanceScore(), result.accuracyScore(),
                result.completenessScore(), result.clarityScore(), result.communicationScore(),
                result.depthScore(), result.overallScore())) {
            if (!Double.isFinite(score) || score < 0 || score > 10) {
                throw new AIResponseValidationException("Gemini evaluation scores must be between 0 and 10");
            }
        }
    }

    private void validateReport(ReportPayload result, InterviewReportContext context) {
        if (result.overallScore() == null || result.technicalScore() == null
                || result.communicationScore() == null || result.problemSolvingScore() == null
                || result.projectKnowledgeScore() == null || result.answerQualityScore() == null
                || result.executiveSummary() == null || result.strengths() == null
                || result.areasToImprove() == null || result.strugglingQuestionOrders() == null
                || result.recommendedTopics() == null || result.improvementSuggestions() == null
                || result.claimVerifications() == null) {
            throw new AIResponseValidationException("Gemini returned an incomplete interview report");
        }
        for (Double score : List.of(result.overallScore(), result.technicalScore(),
                result.communicationScore(), result.problemSolvingScore(), result.projectKnowledgeScore(),
                result.answerQualityScore())) {
            if (!Double.isFinite(score) || score < 0 || score > 100) {
                throw new AIResponseValidationException("Gemini report scores must be between 0 and 100");
            }
        }
        List<Integer> availableOrders = context.answers().stream()
                .map(InterviewAnswerContext::questionOrder).toList();
        if (result.strugglingQuestionOrders().stream().anyMatch(order -> !availableOrders.contains(order))) {
            throw new AIResponseValidationException("Gemini report referenced an unknown question order");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AIResponseValidationException("Could not serialize interview context", ex);
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "provider unavailable" : message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuestionPayload(
            @JsonProperty("question") @JsonAlias("questionText") String question,
            String category,
            @JsonProperty("skill") @JsonAlias("targetSkill") String skill,
            String difficulty,
            String questionType,
            String reason,
            List<String> expectedKeyPoints,
            List<String> groundingEvidence,
            Boolean followUp) {}

    private record EvaluationPayload(
            Double relevanceScore,
            Double accuracyScore,
            Double completenessScore,
            Double clarityScore,
            Double communicationScore,
            Double depthScore,
            Double overallScore,
            Boolean answeredQuestion,
            List<String> strengths,
            List<String> missingPoints,
            String feedback,
            String suggestedImprovement,
            Boolean followUpNeeded,
            String followUpTopic,
            String modelAnswer) {}

    private record ReportPayload(
            Double overallScore,
            Double technicalScore,
            Double communicationScore,
            Double problemSolvingScore,
            Double projectKnowledgeScore,
            Double answerQualityScore,
            String executiveSummary,
            List<String> strengths,
            List<String> areasToImprove,
            List<Integer> strugglingQuestionOrders,
            List<String> recommendedTopics,
            List<String> improvementSuggestions,
            List<String> claimVerifications) {}
}
