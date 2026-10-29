package com.smartinterview.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAIServiceTest {

    private static final String ENDPOINT = "https://test.generativelanguage.local/v1beta/models/test-model:generateContent";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesAndValidatesGroundedStructuredQuestion() throws Exception {
        String evidence = "Project: Built a Spring Boot interview platform.";
        String generated = objectMapper.writeValueAsString(Map.of(
                "questionText", "Your resume says \"" + evidence + "\". How did you structure its API?",
                "category", "Resume project",
                "targetSkill", "Spring Boot",
                "expectedKeyPoints", List.of("Describe API boundaries", "Explain a design choice"),
                "groundingEvidence", List.of(evidence),
                "followUp", false));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAIService service = service(builder);
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(geminiResponse(generated), MediaType.APPLICATION_JSON));

        InterviewContext context = context(evidence);
        InterviewQuestionResult result = service.generateInterviewQuestionResult(context);

        assertEquals("Spring Boot", result.targetSkill());
        assertEquals(List.of(evidence), result.groundingEvidence());
        assertFalse(result.expectedKeyPoints().isEmpty());
        server.verify();
    }

    @Test
    void rejectsQuestionEvidenceNotPresentInCandidateContext() throws Exception {
        String generated = objectMapper.writeValueAsString(Map.of(
                "questionText", "Your project served one million users. How did you scale it?",
                "category", "Resume project",
                "targetSkill", "System Design",
                "expectedKeyPoints", List.of("Discuss scaling"),
                "groundingEvidence", List.of("served one million users"),
                "followUp", false));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAIService service = service(builder);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse(generated), MediaType.APPLICATION_JSON));

        assertThrows(AIResponseValidationException.class,
                () -> service.generateInterviewQuestionResult(context("Project: Built a Spring Boot app.")));
        server.verify();
    }

    @Test
    void reportsProviderFailureWithoutMockFallback() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAIService service = service(builder);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(AIProviderException.class,
                () -> service.generateInterviewQuestionResult(context("Spring Boot project")));
        server.verify();
    }

    @Test
    void validatesStructuredEvaluationScores() throws Exception {
        String generated = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("relevanceScore", 7), Map.entry("accuracyScore", 7),
                Map.entry("completenessScore", 6), Map.entry("clarityScore", 8),
                Map.entry("communicationScore", 7), Map.entry("depthScore", 6),
                Map.entry("overallScore", 7), Map.entry("answeredQuestion", true),
                Map.entry("strengths", List.of("Accurate API explanation")),
                Map.entry("missingPoints", List.of("Authentication flow")),
                Map.entry("feedback", "Relevant response."),
                Map.entry("suggestedImprovement", "Add authentication details."),
                Map.entry("followUpNeeded", true), Map.entry("followUpTopic", "JWT"),
                Map.entry("modelAnswer", "Explain authorization and token validation.")));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAIService service = service(builder);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse(generated), MediaType.APPLICATION_JSON));

        AnswerEvaluationResult result = service.evaluateAnswer("How did you secure the API?", "JWT.",
                context("Spring Boot API"), List.of("JWT authentication", "Authorization rules"));

        assertEquals(7, result.relevanceScore());
        assertTrue(result.followUpNeeded());
        assertEquals(List.of("Authentication flow"), result.missingPoints());
        server.verify();
    }

    @Test
    void generatesValidatedStructuredFinalReport() throws Exception {
        String generated = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("overallScore", 78), Map.entry("technicalScore", 80),
                Map.entry("communicationScore", 74), Map.entry("problemSolvingScore", 76),
                Map.entry("projectKnowledgeScore", 82), Map.entry("answerQualityScore", 78),
                Map.entry("executiveSummary", "Good backend fundamentals; expand design trade-offs."),
                Map.entry("strengths", List.of("Spring Boot fundamentals")),
                Map.entry("areasToImprove", List.of("Database indexing")),
                Map.entry("strugglingQuestionOrders", List.of(2)),
                Map.entry("recommendedTopics", List.of("Database indexing")),
                Map.entry("improvementSuggestions", List.of("Practice explaining query plans.")),
                Map.entry("claimVerifications", List.of("Resume project details were discussed."))));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAIService service = service(builder);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse(generated), MediaType.APPLICATION_JSON));
        AnswerEvaluationResult evaluation = new AnswerEvaluationResult(7, 7, 6, 7, 7, 6,
                7, true, List.of("Explained the API"), List.of("Discuss indexing"),
                "Relevant", "Give a query example", true, "Indexing", "", "{}");
        InterviewReportContext context = new InterviewReportContext("Java Backend Developer", "TECHNICAL",
                "Spring Boot project", "Backend role", List.of(
                new InterviewAnswerContext(1, "Explain REST", "Spring Boot REST API", List.of("REST"), evaluation),
                new InterviewAnswerContext(2, "Explain database indexing", "It speeds queries", List.of("Index"), evaluation)));

        InterviewReportResult result = service.generateInterviewReport(context);

        assertEquals(78, result.overallScore());
        assertEquals(List.of(2), result.strugglingQuestionOrders());
        assertEquals(List.of("Database indexing"), result.recommendedTopics());
        server.verify();
    }

    private GeminiAIService service(RestClient.Builder builder) {
        return new GeminiAIService(builder, objectMapper, "test-key", "test-model",
                "https://test.generativelanguage.local");
    }

    private InterviewContext context(String evidence) {
        return new InterviewContext("RESUME_BASED", "Java Backend Developer", List.of("Java", "Spring Boot"),
                "MEDIUM", List.of(), List.of(), List.of(), "Backend candidate", evidence,
                List.of(evidence), 4.0, "Backend Developer", "Build Java services", List.of("Spring Boot"), 1, 5, null);
    }

    private String geminiResponse(String modelText) throws Exception {
        return objectMapper.writeValueAsString(Map.of("candidates", List.of(Map.of("content",
                Map.of("parts", List.of(Map.of("text", modelText)))))));
    }
}
