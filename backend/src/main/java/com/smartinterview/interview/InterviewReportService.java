package com.smartinterview.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.ai.AIService;
import com.smartinterview.ai.AnswerEvaluationResult;
import com.smartinterview.ai.InterviewAnswerContext;
import com.smartinterview.ai.InterviewReportContext;
import com.smartinterview.ai.InterviewReportResult;
import com.smartinterview.dto.interview.InterviewReportResponse;
import com.smartinterview.entity.AnswerEvaluation;
import com.smartinterview.entity.CandidateAnswer;
import com.smartinterview.entity.InterviewQuestion;
import com.smartinterview.entity.InterviewReport;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.User;
import com.smartinterview.entity.enums.InterviewStatus;
import com.smartinterview.exception.ResourceNotFoundException;
import com.smartinterview.repository.InterviewQuestionRepository;
import com.smartinterview.repository.InterviewReportRepository;
import com.smartinterview.repository.InterviewSessionRepository;
import com.smartinterview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewReportService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public InterviewReportResponse generateReport(Long sessionId, String userEmail) {
        InterviewSession session = loadSessionForUser(sessionId, userEmail);
        if (session.getStatus() != InterviewStatus.COMPLETED) {
            throw new IllegalStateException("The interview report is available after the session is completed");
        }
        return reportRepository.findByInterviewSessionId(sessionId)
                .map(this::toResponse)
                .orElseGet(() -> createReport(session));
    }

    @Transactional(readOnly = true)
    public InterviewReportResponse getReport(Long sessionId, String userEmail) {
        loadSessionForUser(sessionId, userEmail);
        InterviewReport report = reportRepository.findByInterviewSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview report for session", sessionId));
        return toResponse(report);
    }

    private InterviewReportResponse createReport(InterviewSession session) {
        List<InterviewQuestion> questions = questionRepository
                .findByInterviewSessionIdOrderByQuestionOrderAsc(session.getId());
        List<InterviewAnswerContext> answers = questions.stream()
                .filter(question -> question.getCandidateAnswer() != null)
                .map(this::toAnswerContext)
                .toList();
        if (answers.isEmpty() || answers.size() != session.getCompletedQuestionCount()) {
            throw new IllegalStateException("The interview does not contain all expected evaluated answers");
        }

        InterviewReportContext context = new InterviewReportContext(
                session.getTargetRole(),
                session.getInterviewType().name(),
                session.getResume() != null ? session.getResume().getRawText() : null,
                session.getJobDescription() != null ? session.getJobDescription().getRawText() : null,
                answers);
        InterviewReportResult result = aiService.generateInterviewReport(context);

        InterviewReport report = InterviewReport.builder()
                .interviewSession(session)
                .overallScore(result.overallScore())
                .technicalScore(result.technicalScore())
                .communicationScore(result.communicationScore())
                .problemSolvingScore(result.problemSolvingScore())
                .executiveSummary(result.executiveSummary())
                .strengthsJson(toJson(result.strengths()))
                .weaknessesJson(toJson(Map.of(
                        "areasToImprove", result.areasToImprove(),
                        "strugglingQuestionOrders", result.strugglingQuestionOrders())))
                .claimVerificationsJson(toJson(Map.of(
                        "projectKnowledgeScore", result.projectKnowledgeScore(),
                        "claims", result.claimVerifications())))
                .preparationRoadmapJson(toJson(Map.of(
                        "answerQualityScore", result.answerQualityScore(),
                        "recommendedTopics", result.recommendedTopics(),
                        "improvementSuggestions", result.improvementSuggestions())))
                .generatedAt(LocalDateTime.now())
                .build();
        return toResponse(reportRepository.save(report));
    }

    private InterviewAnswerContext toAnswerContext(InterviewQuestion question) {
        CandidateAnswer answer = question.getCandidateAnswer();
        AnswerEvaluation evaluation = answer.getEvaluation();
        if (evaluation == null) {
            throw new IllegalStateException("An answer is missing its evaluation");
        }
        return new InterviewAnswerContext(question.getQuestionOrder(), question.getQuestionText(),
                answer.getAnswerText(), readStringList(question.getExpectedKeyPointsJson()),
                toEvaluationResult(evaluation));
    }

    private AnswerEvaluationResult toEvaluationResult(AnswerEvaluation evaluation) {
        try {
            return objectMapper.readValue(evaluation.getDetailedFeedback(), AnswerEvaluationResult.class);
        } catch (Exception ignored) {
            return new AnswerEvaluationResult(
                    number(evaluation.getOverallScore()),
                    number(evaluation.getTechnicalAccuracyScore()),
                    number(evaluation.getCompletenessScore()),
                    number(evaluation.getClarityScore()),
                    number(evaluation.getClarityScore()),
                    number(evaluation.getTechnicalAccuracyScore()),
                    number(evaluation.getOverallScore()),
                    true,
                    evaluation.getStrengths() == null ? List.of() : List.of(evaluation.getStrengths()),
                    evaluation.getAreasOfImprovement() == null ? List.of() : List.of(evaluation.getAreasOfImprovement()),
                    evaluation.getDetailedFeedback(),
                    evaluation.getAreasOfImprovement(),
                    false,
                    evaluation.getFollowUpTopic(),
                    evaluation.getAreasOfImprovement(),
                    null);
        }
    }

    private InterviewSession loadSessionForUser(Long sessionId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", sessionId));
    }

    private InterviewReportResponse toResponse(InterviewReport report) {
        JsonNode weaknesses = readNode(report.getWeaknessesJson());
        JsonNode claims = readNode(report.getClaimVerificationsJson());
        JsonNode roadmap = readNode(report.getPreparationRoadmapJson());
        return InterviewReportResponse.builder()
                .reportId(report.getId())
                .sessionId(report.getInterviewSession().getId())
                .overallScore(number(report.getOverallScore()))
                .technicalScore(number(report.getTechnicalScore()))
                .communicationScore(number(report.getCommunicationScore()))
                .problemSolvingScore(number(report.getProblemSolvingScore()))
                .projectKnowledgeScore(number(claims.path("projectKnowledgeScore").asDouble(0)))
                .answerQualityScore(number(roadmap.path("answerQualityScore").asDouble(0)))
                .executiveSummary(report.getExecutiveSummary())
                .strengths(readStringList(report.getStrengthsJson()))
                .areasToImprove(readStringArrayOrField(weaknesses, "areasToImprove"))
                .strugglingQuestionOrders(readIntegerArrayOrField(weaknesses, "strugglingQuestionOrders"))
                .recommendedTopics(readStringArrayOrField(roadmap, "recommendedTopics"))
                .improvementSuggestions(readStringArrayOrField(roadmap, "improvementSuggestions"))
                .claimVerifications(readStringArrayOrField(claims, "claims"))
                .generatedAt(report.getGeneratedAt())
                .build();
    }

    private JsonNode readNode(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private List<String> readStringArrayOrField(JsonNode node, String field) {
        JsonNode value = node.isArray() ? node : node.path(field);
        if (!value.isArray()) return List.of();
        return objectMapper.convertValue(value, new TypeReference<>() {});
    }

    private List<Integer> readIntegerArrayOrField(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) return List.of();
        return objectMapper.convertValue(value, new TypeReference<>() {});
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize interview report", ex);
        }
    }

    private double number(Double value) {
        return value == null ? 0 : value;
    }
}
