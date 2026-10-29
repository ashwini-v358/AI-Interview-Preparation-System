package com.smartinterview.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Verifies the Gemini → MockAIService degradation path.
 *
 * The Gemini HTTP layer is always mocked, so these tests are deterministic and never reach the
 * network. The assertion that matters most for the product is that a provider failure yields a
 * usable, deterministic question rather than an exception reaching the candidate.
 */
class FallbackAIServiceTest {

    private static final String BASE_URL = "https://test.generativelanguage.local";
    private static final String ENDPOINT = BASE_URL + "/v1beta/models/test-model:generateContent";
    private static final String PLACEHOLDER_KEY = "unit-test-placeholder-key";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InterviewContext context = context();

    @Test
    void servesTheGeminiResponseWhenTheProviderSucceeds() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse(objectMapper.writeValueAsString(Map.of(
                        "question", "You used Spring Data JPA in the Smart Interview Platform. "
                                + "How would you reduce query count when fetching an order with its items?",
                        "skill", "JPA/Hibernate",
                        "difficulty", "MEDIUM",
                        "questionType", "SCENARIO",
                        "reason", "JPA is a required job skill that is currently a detected gap.",
                        "expectedKeyPoints", List.of("Use fetch joins", "Verify with SQL logs"),
                        "groundingEvidence", List.of("Smart Interview Platform"),
                        "followUp", false))), MediaType.APPLICATION_JSON));
        FallbackAIService service = new FallbackAIService(gemini(builder), new MockAIService(objectMapper), true);

        InterviewQuestionResult result = service.generateInterviewQuestionResult(context);

        assertEquals("JPA/Hibernate", result.targetSkill());
        assertEquals("SCENARIO", result.questionType());
        assertTrue(result.reason().contains("detected gap"), result.reason());
        assertEquals(0, service.fallbackCount());
        server.verify();
    }

    @Test
    void fallsBackToTheDeterministicMockOnProviderFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        FallbackAIService service = new FallbackAIService(gemini(builder), new MockAIService(objectMapper), true);

        InterviewQuestionResult result = service.generateInterviewQuestionResult(context);

        assertNotNull(result.questionText());
        assertFalse(result.questionText().isBlank());
        assertFalse(result.expectedKeyPoints().isEmpty());
        assertEquals(1, service.fallbackCount());
        server.verify();
    }

    @Test
    void fallsBackOnRateLimit() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        FallbackAIService service = new FallbackAIService(gemini(builder), new MockAIService(objectMapper), true);

        assertNotNull(service.generateInterviewQuestionResult(context).questionText());
        assertEquals(1, service.fallbackCount());
        server.verify();
    }

    @Test
    void fallsBackOnMalformedOrIncompleteStructuredResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"question\":\"Explain JPA batching.\"}", MediaType.APPLICATION_JSON));
        FallbackAIService service = new FallbackAIService(gemini(builder), new MockAIService(objectMapper), true);

        assertNotNull(service.generateInterviewQuestionResult(context).questionText());
        assertEquals(1, service.fallbackCount());
        server.verify();
    }

    @Test
    void fallsBackWhenNoApiKeyIsConfigured() {
        GeminiAIService gemini = new GeminiAIService(RestClient.builder(), objectMapper,
                "", "test-model", BASE_URL);
        FallbackAIService service = new FallbackAIService(gemini, new MockAIService(objectMapper), true);

        assertNotNull(service.generateInterviewQuestionResult(context).questionText());
        assertEquals(1, service.fallbackCount());
    }

    @Test
    void fallsBackForAnswerEvaluationAndReportSoTheInterviewContinues() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        FallbackAIService service = new FallbackAIService(gemini(builder), new MockAIService(objectMapper), true);

        AnswerEvaluationResult evaluation = service.evaluateAnswer(
                "How did you secure the API?", "JWT protects each request.", context, List.of("JWT", "RBAC"));
        assertTrue(evaluation.overallScore() >= 0);
        assertEquals(1, service.fallbackCount());
        server.verify();
    }

    @Test
    void propagatesTheFailureWhenFallbackIsDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        FallbackAIService service = new FallbackAIService(gemini(builder), new MockAIService(objectMapper), false);

        assertThrows(AIProviderException.class,
                () -> service.generateInterviewQuestionResult(context));
        assertEquals(0, service.fallbackCount());
        server.verify();
    }

    @Test
    void mockFallbackIsDeterministicAcrossRepeatedCalls() {
        GeminiAIService gemini = new GeminiAIService(RestClient.builder(), objectMapper,
                "", "test-model", BASE_URL);
        FallbackAIService service = new FallbackAIService(gemini, new MockAIService(objectMapper), true);

        String first = service.generateInterviewQuestionResult(context).questionText();
        String second = service.generateInterviewQuestionResult(context).questionText();

        assertEquals(first, second);
        assertEquals(2, service.fallbackCount());
    }

    private GeminiAIService gemini(RestClient.Builder builder) {
        return new GeminiAIService(builder, objectMapper, PLACEHOLDER_KEY, "test-model", BASE_URL);
    }

    private String geminiResponse(String modelText) {
        try {
            return objectMapper.writeValueAsString(Map.of("candidates", List.of(Map.of("content",
                    Map.of("parts", List.of(Map.of("text", modelText)))))));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private InterviewContext context() {
        String project = "Project: Smart Interview Platform built with Spring Boot and MySQL.";
        return new InterviewContext(
                "COMPREHENSIVE_ADAPTIVE", "Java Backend Developer", List.of("Java", "Spring Boot"),
                "MEDIUM", List.of(), List.of(), List.of(), "Backend engineer", project,
                List.of(project), 4.0, "Java Backend Developer", "Java and Spring Boot role",
                List.of("Java", "Spring Boot"), 1, 5, "SQL",
                List.of(new InterviewSkillSignal("SQL", "MISSING", null, "job: SQL")),
                List.of(new InterviewSkillSignal("SQL", "WEAK", 25.0, "Previous average 25%")),
                List.of(new InterviewSkillSignal("SQL", "IN_PREPARATION", null, "Priority HIGH")),
                List.of("SQL is required for this role."));
    }
}
