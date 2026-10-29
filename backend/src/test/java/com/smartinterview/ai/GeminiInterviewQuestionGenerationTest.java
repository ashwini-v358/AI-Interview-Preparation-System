package com.smartinterview.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Verifies Gemini request construction, structured response parsing and validation rules.
 *
 * Every case mocks the HTTP layer with {@link MockRestServiceServer}, so no test performs a real
 * Gemini call, no test depends on network availability, and no test contains a usable API key.
 */
class GeminiInterviewQuestionGenerationTest {

    private static final String BASE_URL = "https://test.generativelanguage.local";
    private static final String ENDPOINT = BASE_URL + "/v1beta/models/test-model:generateContent";
    private static final String PLACEHOLDER_KEY = "unit-test-placeholder-key";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStructuredQuestionResponseWithTypeDifficultyAndReason() throws Exception {
        Harness harness = harness();
        expectJson(harness, Map.of(
                "question", "You used Spring Data JPA in the Smart Interview Platform. Suppose fetching an "
                        + "order with its items suddenly issues a large number of queries. How would you "
                        + "identify the problem and improve the implementation?",
                "skill", "JPA/Hibernate",
                "difficulty", "MEDIUM",
                "questionType", "SCENARIO",
                "reason", "JPA was a missing skill in the latest skill-gap analysis and is required by the "
                        + "target job description, while preparation for it is still incomplete.",
                "expectedKeyPoints", List.of("Identify the N+1 query pattern",
                        "Use fetch joins or entity graphs", "Verify with SQL logging and a query count"),
                "groundingEvidence", List.of("Project: Smart Interview Platform"),
                "followUp", false));

        InterviewQuestionResult result = service(harness).generateInterviewQuestionResult(context());

        assertEquals("JPA/Hibernate", result.targetSkill());
        assertEquals("SCENARIO", result.questionType());
        assertEquals("MEDIUM", result.difficulty());
        assertEquals("SCENARIO", result.category());
        assertTrue(result.reason().contains("skill-gap analysis"), result.reason());
        assertEquals(3, result.expectedKeyPoints().size());
        assertFalse(result.followUp());
        harness.server().verify();
    }

    @Test
    void sendsCandidateContextIncludingGapsPerformancePreparationAndAskedQuestions() throws Exception {
        Harness harness = harness();
        harness.server().expect(requestTo(ENDPOINT))
                .andExpect(header("x-goog-api-key", PLACEHOLDER_KEY))
                .andExpect(content().string(containsString("REAL technical interviewer")))
                .andExpect(content().string(containsString("skillGapSignals")))
                .andExpect(content().string(containsString("performanceSignals")))
                .andExpect(content().string(containsString("preparationSignals")))
                .andExpect(content().string(containsString("selectionRationales")))
.andExpect(content().string(containsString("previousQuestions")))
                .andExpect(content().string(containsString("Explain how transaction propagation works")))
                .andExpect(content().string(containsString("MISSING")))
                .andExpect(content().string(containsString("WEAK")))
                .andExpect(content().string(containsString("IN_PREPARATION")))
                .andExpect(content().string(containsString("SQL")))
                .andRespond(withSuccess(geminiResponse(writeJson(Map.of(
                        "question", "How would you index this table?",
                        "skill", "SQL",
                        "difficulty", "EASY",
                        "questionType", "TECHNICAL",
                        "reason", "SQL is a required role skill the candidate has not prepared yet.",
                        "expectedKeyPoints", List.of("Choose a column", "Explain the index benefit"),
                        "groundingEvidence", List.of(),
                        "followUp", false))), MediaType.APPLICATION_JSON));

        InterviewQuestionResult result = service(harness).generateInterviewQuestionResult(context());

        assertEquals("SQL", result.targetSkill());
        harness.server().verify();
    }

    @Test
    void parsesQuestionWrappedInMarkdownCodeFence() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "question", "Describe how you would secure this endpoint.",
                "skill", "Spring Security",
                "difficulty", "MEDIUM",
                "questionType", "TECHNICAL",
                "reason", "Authentication is required by the job description.",
                "expectedKeyPoints", List.of("Explain token validation"),
                "groundingEvidence", List.of(),
                "followUp", false));
        Harness harness = harness();
        harness.server().expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse("```json\n" + payload + "\n```"), MediaType.APPLICATION_JSON));

        InterviewQuestionResult result = service(harness).generateInterviewQuestionResult(context());

        assertEquals("Spring Security", result.targetSkill());
        assertEquals("TECHNICAL", result.questionType());
        harness.server().verify();
    }

    @Test
    void rejectsMalformedJsonWithoutCrashingTheInterview() {
        Harness harness = harness();
        harness.server().expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse("{ this is not json "), MediaType.APPLICATION_JSON));

        assertThrows(AIResponseValidationException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void rejectsMissingRequiredFields() {
        Harness harness = harness();
        expectJson(harness, Map.of(
                "question", "Explain JPA batching.",
                "skill", "JPA",
                "groundingEvidence", List.of()));

        assertThrows(AIResponseValidationException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void rejectsEmptyQuestion() {
        Harness harness = harness();
        expectJson(harness, Map.of(
                "question", "   ",
                "skill", "JPA",
                "expectedKeyPoints", List.of("Explain batching"),
                "groundingEvidence", List.of()));

        assertThrows(AIResponseValidationException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void rejectsUnsupportedQuestionType() {
        Harness harness = harness();
        expectJson(harness, Map.of(
                "question", "Explain connection pooling.",
                "skill", "HikariCP",
                "difficulty", "MEDIUM",
                "questionType", "RIDDLE",
                "expectedKeyPoints", List.of("Explain pool sizing"),
                "groundingEvidence", List.of()));

        assertThrows(AIResponseValidationException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void rejectsUnsupportedDifficultyLevel() {
        Harness harness = harness();
        expectJson(harness, Map.of(
                "question", "Explain connection pooling.",
                "skill", "HikariCP",
                "difficulty", "IMPOSSIBLE",
                "questionType", "TECHNICAL",
                "expectedKeyPoints", List.of("Explain pool sizing"),
                "groundingEvidence", List.of()));

        assertThrows(AIResponseValidationException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void rejectsGroundingEvidenceNotPresentInTheCandidateContext() {
        Harness harness = harness();
        expectJson(harness, Map.of(
                "question", "How did you handle two million concurrent users?",
                "skill", "System Design",
                "expectedKeyPoints", List.of("Discuss scaling"),
                "groundingEvidence", List.of("two million concurrent users"),
                "followUp", false));

        assertThrows(AIResponseValidationException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void reportsRateLimitAsProviderFailureSoCallerCanFallBack() {
        Harness harness = harness();
        harness.server().expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(AIProviderException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void reportsServerErrorAsProviderFailureSoCallerCanFallBack() {
        Harness harness = harness();
        harness.server().expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(AIProviderException.class,
                () -> service(harness).generateInterviewQuestionResult(context()));
        harness.verify();
    }

    @Test
    void reportsMissingApiKeyAsProviderFailureRatherThanStartingBroken() {
        GeminiAIService service = new GeminiAIService(RestClient.builder(), objectMapper,
                "   ", "test-model", BASE_URL);

        AIProviderException failure = assertThrows(AIProviderException.class,
                () -> service.generateInterviewQuestionResult(context()));

        assertTrue(failure.getMessage().contains("no API key"), failure.getMessage());
    }

    @Test
    void neverEchoesTheApiKeyInFailureMessages() {
        Harness harness = harness();
        harness.server().expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        GeminiAIService service = service(harness);

        AIProviderException failure = assertThrows(AIProviderException.class,
                () -> service.generateInterviewQuestionResult(context()));

        assertFalse(failure.getMessage().contains(PLACEHOLDER_KEY), failure.getMessage());
        assertFalse(String.valueOf(failure).contains(PLACEHOLDER_KEY), String.valueOf(failure));
    }

    // ─── Harness ─────────────────────────────────────────────────────────────

    private Harness harness() {
        RestClient.Builder builder = RestClient.builder();
        return new Harness(builder, MockRestServiceServer.bindTo(builder).build());
    }

    private void expectJson(Harness harness, Map<String, Object> payload) {
        harness.server().expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse(writeJson(payload)), MediaType.APPLICATION_JSON));
    }

    private GeminiAIService service(Harness harness) {
        return new GeminiAIService(harness.builder(), objectMapper, PLACEHOLDER_KEY,
                "test-model", BASE_URL);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String geminiResponse(String modelText) {
        return writeJson(Map.of("candidates", List.of(Map.of("content",
                Map.of("parts", List.of(Map.of("text", modelText)))))));
    }

    private record Harness(RestClient.Builder builder, MockRestServiceServer server) {
        private void verify() {
            server.verify();
        }
    }

    private InterviewContext context() {
        String project = "Project: Smart Interview Platform built with Spring Boot and MySQL.";
        return new InterviewContext(
                "COMPREHENSIVE_ADAPTIVE", "Java Backend Developer", List.of("Java", "Spring Boot"),
                "MEDIUM",
                List.of("Explain how transaction propagation works"),
                List.of("I used @Transactional but did not read the propagation rules."),
                List.of(),
                "Backend engineer with Spring Boot experience",
                project,
                List.of(project),
                4.0, "Java Backend Developer", "Java, Spring Boot, REST, JPA, MySQL, SQL",
                List.of("Java", "Spring Boot", "SQL"), 2, 5, "SQL",
                List.of(new InterviewSkillSignal("SQL", "MISSING", null, "job: SQL")),
                List.of(new InterviewSkillSignal("JPA/Hibernate", "WEAK", 25.0, "Previous interview average 25%")),
                List.of(new InterviewSkillSignal("SQL", "IN_PREPARATION", null, "Preparation priority HIGH")),
                List.of("SQL is a required skill for your selected job description."));
    }
}
